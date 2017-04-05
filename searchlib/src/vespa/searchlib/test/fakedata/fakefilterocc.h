// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include "fakeword.h"
#include "fakeposting.h"

namespace search {

namespace fakedata {

/*
 * Old posocc format.
 */
class FakeFilterOcc : public FakePosting
{
private:
    std::vector<uint32_t> _uncompressed;
    unsigned int _docIdLimit;
    unsigned int _hitDocs;
public:
    FakeFilterOcc(const FakeWord &fakeword);

    ~FakeFilterOcc();

    static void forceLink();

    size_t bitSize() const override;
    bool hasWordPositions(void) const override;
    int lowLevelSinglePostingScan(void) const override;
    int lowLevelSinglePostingScanUnpack(void) const override;
    int lowLevelAndPairPostingScan(const FakePosting &rhs) const override;
    int lowLevelAndPairPostingScanUnpack(const FakePosting &rhs) const override;
    queryeval::SearchIterator * createIterator(const fef::TermFieldMatchDataArray &matchData) const override;
};

} // namespace fakedata

} // namespace search
