// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#include <vespa/fastos/fastos.h>
#include "metric.h"

#include "countmetric.h"
#include "valuemetric.h"
#include "metricset.h"
#include "namehash.h"
#include <vespa/vespalib/text/stringtokenizer.h>
#include <vespa/vespalib/util/exceptions.h>

#include <algorithm>
#include <iterator>

namespace metrics {

bool
MetricVisitor::visitCountMetric(const AbstractCountMetric& m,
                                bool autoGenerated)
{
    return visitMetric(m, autoGenerated);
}

bool
MetricVisitor::visitValueMetric(const AbstractValueMetric& m,
                                bool autoGenerated)
{
    return visitMetric(m, autoGenerated);
}

bool
MetricVisitor::visitMetric(const Metric&, bool)
{
    throw vespalib::IllegalStateException(
            "visitMetric called with default implementation. You should either "
            "override specific visit functions or this catchall function.",
            VESPA_STRLOC);
}

namespace {
    Metric::Tags legacyTagStringToKeyedTags(const std::string& tagStr) {
        vespalib::StringTokenizer tokenizer(tagStr, " \t\r\f");
        Metric::Tags tags;
        std::transform(tokenizer.getTokens().begin(),
                       tokenizer.getTokens().end(),
                       std::back_inserter(tags),
                       [](const std::string& s) { return Tag(s, ""); });
        return tags;
    }
    std::string namePattern = "[a-zA-Z][_a-zA-Z0-9]*";
}

vespalib::Regexp Metric::_namePattern(namePattern);

Metric::Metric(const String& name,
               const String& tags,
               const String& description,
               MetricSet* owner)
    : _name(name),
      _description(description),
      _tags(legacyTagStringToKeyedTags(tags)),
      _owner(nullptr) // Set later by registry
{
    verifyConstructionParameters();
    assignMangledNameWithDimensions();
    registerWithOwnerIfRequired(owner);
}

Metric::Metric(const String& name,
               Tags dimensions,
               const String& description,
               MetricSet* owner)
    : _name(name),
      _description(description),
      _tags(std::move(dimensions)),
      _owner(nullptr)
{
    verifyConstructionParameters();
    assignMangledNameWithDimensions();
    registerWithOwnerIfRequired(owner);
}


Metric::Metric(const Metric& other, MetricSet* owner)
    : Printable(other),
      _name(other._name),
      _description(other._description),
      _tags(other._tags),
      _owner(nullptr)
{
    assignMangledNameWithDimensions();
    registerWithOwnerIfRequired(owner);
}

Metric::~Metric() { }

bool
Metric::tagsSpecifyAtLeastOneDimension(const Tags& tags) const
{
    auto hasNonEmptyTagValue = [](const Tag& t) { return !t.value.empty(); };
    return std::any_of(tags.begin(), tags.end(), hasNonEmptyTagValue);
}

void
Metric::assignMangledNameWithDimensions()
{
    if (!tagsSpecifyAtLeastOneDimension(_tags)) {
        return;
    }
    sortTagsInDeterministicOrder();
    _mangledName = createMangledNameWithDimensions();
}

void
Metric::sortTagsInDeterministicOrder()
{
    std::sort(_tags.begin(), _tags.end(), [](const Tag& a, const Tag& b) {
        return a.key < b.key;
    });
}

std::string
Metric::createMangledNameWithDimensions() const
{
    vespalib::asciistream s;
    s << _name << '{';
    const size_t sz = _tags.size();
    for (size_t i = 0; i < sz; ++i) {
        const Tag& dimension(_tags[i]);
        if (dimension.value.empty()) {
            continue;
        }
        if (i != 0) {
            s << ',';
        }
        s << dimension.key << ':' << dimension.value;
    }
    s << '}';
    return s.str();
}

void
Metric::verifyConstructionParameters()
{
    if (_name.size() == 0) {
        throw vespalib::IllegalArgumentException(
                "Metric cannot have empty name", VESPA_STRLOC);
    }
    if (!_namePattern.match(_name)) {
        throw vespalib::IllegalArgumentException(
                "Illegal metric name '" + _name + "'. Names must match pattern "
                + namePattern, VESPA_STRLOC);
    }
}

void
Metric::registerWithOwnerIfRequired(MetricSet* owner)
{
    if (owner) {
        owner->registerMetric(*this);
    }
}

const MetricSet*
Metric::getRoot() const
{
    return (_owner == 0 ? (isMetricSet() ? static_cast<const MetricSet*>(this)
                                         : 0)
                        : _owner->getRoot());
}

vespalib::string
Metric::getPath() const
{
    if (_owner == 0 || _owner->_owner == 0) {
        return _name;
    } else {
        return _owner->getPath() + "." + _name;
    }
}

std::vector<Metric::String>
Metric::getPathVector() const
{
    std::vector<String> result;
    result.push_back(_name);
    const MetricSet* owner(_owner);
    while (owner != 0) {
        result.push_back(owner->_name);
        owner = owner->_owner;
    }
    std::reverse(result.begin(), result.end());
    return result;
}

bool
Metric::hasTag(const String& tag) const
{
    return std::find_if(_tags.begin(), _tags.end(), [&](const Tag& t) {
        return t.key == tag;
    }) != _tags.end();
}

void
Metric::addMemoryUsage(MemoryConsumption& mc) const
{
    ++mc._metricCount;
    mc._metricName += mc.getStringMemoryUsage(_name, mc._metricNameUnique);
    mc._metricDescription += mc.getStringMemoryUsage(
                                    _description, mc._metricDescriptionUnique);
    mc._metricTagCount += _tags.size();
    // XXX figure out what we actually want to report from tags here...
    // XXX we don't care about unique strings since they don't matter anymore.
    //mc._metricTags += mc.getStringMemoryUsage(_tags, mc._metricTagsUnique);
    mc._metricMeta += sizeof(Metric);
}

void
Metric::updateNames(NameHash& hash) const
{
    Metric& m(const_cast<Metric&>(*this));
    hash.updateName(m._name);
    hash.updateName(m._description);
    // Tags use vespalib::string which isn't refcounted under the hood and
    // use small string optimizations, meaning the implicit ref sharing hack
    // won't work for them anyway.
}

void
Metric::printDebug(std::ostream& out, const std::string& indent) const
{
    (void) indent;
    out << "name=" << _name << ", instance=" << ((const void*) this)
        << ", owner=" << ((const void*) _owner);
}

Metric*
Metric::assignValues(const Metric& m) {
    std::vector<Metric::LP> ownerList;
    const_cast<Metric&>(m).addToSnapshot(*this, ownerList);
    // As this should only be called among active metrics, all metrics
    // should exist and owner list should thus always end up empty.
    assert(ownerList.empty());
    return this;
}
} // metrics
