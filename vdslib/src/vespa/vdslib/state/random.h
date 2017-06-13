// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include <vespa/vespalib/util/random.h>

namespace storage {
namespace lib {

/**
 * Random number generator. Compatible with java.util.Random
 * Calls PRNG from vespalib, but throws away the first number generated.
 */
class RandomGen : public vespalib::RandomGen{
public:
    RandomGen(int32_t seed) : vespalib::RandomGen(seed) {
        nextDouble();
    };

    /**
     * Construct a random number generator with an auto-generated seed
     */
    RandomGen() : vespalib::RandomGen() {}

    /**
     * Reset the seed
     */
    void setSeed(int32_t seed) {
        vespalib::RandomGen::setSeed(seed);
        nextDouble();
    }
};

} // lib
} // storage

