// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "textwriter.h"
#include "countmetric.h"
#include "metricset.h"
#include "metricsnapshot.h"
#include "valuemetric.h"
#include <sstream>

namespace metrics {

TextWriter::TextWriter(std::ostream& out, uint32_t period,
                       const std::string& regex, bool verbose)
    : _period(period), _out(out), _regex(regex), _verbose(verbose)
{
}

bool
TextWriter::visitSnapshot(const MetricSnapshot& snapshot)
{
    _out << "snapshot \"" << snapshot.getName() << "\" from "
         << snapshot.getFromTime() << " to " << snapshot.getToTime()
         << " period " << snapshot.getPeriod();
    return true;
}

void
TextWriter::doneVisitingSnapshot(const MetricSnapshot&)
{
}

bool
TextWriter::visitMetricSet(const MetricSet& set, bool)
{
    _path.push_back(set.getMangledName());
    return true;
}
void
TextWriter::doneVisitingMetricSet(const MetricSet&) {
    _path.pop_back();
}

bool
TextWriter::writeCommon(const Metric& metric)
{
    std::ostringstream path;
    for (uint32_t i=0; i<_path.size(); ++i) {
        path << _path[i] << ".";
    }
    std::string mypath(path.str());
    path << metric.getMangledName();
    if (_regex.match(path.str())) {
        if (metric.used() || _verbose) {
            _out << "\n" << mypath;
            return true;
        }
    }
    return false;
}

bool
TextWriter::visitCountMetric(const AbstractCountMetric& m, bool)
{
    if (writeCommon(m)) {
        if (_verbose || m.used()) {
            MetricValueClass::UP values(m.getValues());
            _out << m.getMangledName()
                 << (m.sumOnAdd() ? " count=" : " value=");
            values->output("count", _out);
        }
    }
    return true;
}

bool
TextWriter::visitValueMetric(const AbstractValueMetric& m, bool)
{
    if (writeCommon(m)) {
        m.print(_out, _verbose, "  ", _period);
    }
    return true;
}

} // metrics
