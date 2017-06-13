// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

namespace vespalib {
    namespace slime {
        class Inspector;
    }
}

namespace config {

class ConfigPayload {
public:
    ConfigPayload(const ::vespalib::slime::Inspector & inspector)
        : _inspector(inspector)
    {}
    const ::vespalib::slime::Inspector & get() const { return _inspector; }
private:
    const ::vespalib::slime::Inspector & _inspector;
};

} // namespace config

