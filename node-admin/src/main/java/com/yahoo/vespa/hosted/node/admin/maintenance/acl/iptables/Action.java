// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.maintenance.acl.iptables;

/**
 * @author mpolden
 */
public enum Action {
    DROP("DROP"),
    REJECT("REJECT"),
    ACCEPT("ACCEPT");

    public final String name;

    Action(String name) {
        this.name = name;
    }
}
