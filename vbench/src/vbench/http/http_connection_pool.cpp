// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "http_connection_pool.h"

namespace vbench {

HttpConnectionPool::HttpConnectionPool(Timer &timer)
    : _lock(),
      _map(),
      _store(),
      _timer(timer)
{
}

HttpConnectionPool::~HttpConnectionPool() {}

HttpConnection::UP
HttpConnectionPool::getConnection(const ServerSpec &server)
{
    double now = _timer.sample();
    vespalib::LockGuard guard(_lock);
    auto res = _map.insert(std::make_pair(server, _store.size()));
    if (res.second) {
        _store.emplace_back();
    }
    Queue &queue = _store[res.first->second];
    while (!queue.empty() && !queue.front()->mayReuse(now)) {
        queue.pop();
    }
    if (!queue.empty()) {
        HttpConnection::UP ret = std::move(queue.access(0));
        queue.pop();
        return ret;
    }
    return HttpConnection::UP(new HttpConnection(server));
}

void
HttpConnectionPool::putConnection(HttpConnection::UP conn)
{
    double now = _timer.sample();
    vespalib::LockGuard guard(_lock);
    conn->touch(now);
    size_t idx = _map[conn->server()];
    assert(idx < _store.size());
    _store[idx].push(std::move(conn));
}

} // namespace vbench
