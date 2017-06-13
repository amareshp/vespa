// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.feedhandler;

import java.util.Collections;
import java.util.concurrent.Executor;

import com.yahoo.container.jdisc.HttpRequest;
import com.yahoo.container.jdisc.HttpResponse;
import com.yahoo.container.jdisc.ThreadedHttpRequestHandler;
import com.yahoo.search.handler.SearchHandler;

public class VespaFeedHandlerGet extends ThreadedHttpRequestHandler {

    private final SearchHandler searchHandler;

    public VespaFeedHandlerGet(SearchHandler searchHandler, Executor executor) {
        super(executor, null, true);
        this.searchHandler = searchHandler;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        return searchHandler.handle(new HttpRequest(request.getJDiscRequest(), request.getData(), Collections.singletonMap("searchChain", "vespaget")));
    }

}
