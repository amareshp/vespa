// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include "distributorcomponent.h"
#include "distributor_bucket_space.h"

namespace storage {
namespace distributor {

/**
 * Component bound to a specific bucket space, with utility operations to
 * operate on buckets in this space.
 */
class DistributorBucketSpaceComponent : public DistributorComponent {
    DistributorBucketSpace& _bucketSpace;
public:
    DistributorBucketSpaceComponent(DistributorInterface& distributor,
                                    DistributorBucketSpaceRepo &bucketSpaceRepo,
                                    DistributorBucketSpace& bucketSpace,
                                    DistributorComponentRegister& compReg,
                                    const std::string& name);
};

}
}
