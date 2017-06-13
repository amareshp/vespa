// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.messagebus;

import com.yahoo.messagebus.routing.RetryPolicy;
import com.yahoo.messagebus.routing.RetryTransientErrorsPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * To facilitate several configuration parameters to the {@link MessageBus} constructor, all parameters are held by this
 * class. This class has reasonable default values for each parameter.
 *
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class MessageBusParams {

    private final List<Protocol> protocols = new ArrayList<Protocol>();
    private RetryPolicy retryPolicy;
    private int maxPendingCount;
    private int maxPendingSize;

    /**
     * Constructs a new instance of this parameter object with default values for all members.
     */
    public MessageBusParams() {
        retryPolicy = new RetryTransientErrorsPolicy();
        maxPendingCount = 1024;
        maxPendingSize = 128 * 1024 * 1024;
    }

    /**
     * Implements the copy constructor.
     *
     * @param params The object to copy.
     */
    public MessageBusParams(MessageBusParams params) {
        protocols.addAll(params.protocols);
        retryPolicy = params.retryPolicy;
        maxPendingCount = params.maxPendingCount;
        maxPendingSize = params.maxPendingSize;
    }

    /**
     * Returns the retry policy for the resender.
     *
     * @return The policy.
     */
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /**
     * Sets the retry policy for the resender.
     *
     * @param retryPolicy The policy to set.
     * @return This, to allow chaining.
     */
    public MessageBusParams setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * Adds a new protocol to this.
     *
     * @param protocol The protocol to add.
     * @return This, to allow chaining.
     */
    public MessageBusParams addProtocol(Protocol protocol) {
        protocols.add(protocol);
        return this;
    }

    /**
     * Registers multiple protocols with this by calling {@link #addProtocol(Protocol)} multiple times.
     *
     * @param protocols The protocols to register.
     * @return This, to allow chaining.
     */
    public MessageBusParams addProtocols(List<Protocol> protocols) {
        for (Protocol protocol : protocols) {
            addProtocol(protocol);
        }
        return this;
    }

    /**
     * Returns the number of protocols that are contained in this.
     *
     * @return The number of protocols.
     */
    public int getNumProtocols() {
        return protocols.size();
    }

    /**
     * Returns the protocol at the given index.
     *
     * @param i The index of the protocol to return.
     * @return The protocol object.
     */
    public Protocol getProtocol(int i) {
        return protocols.get(i);
    }

    /**
     * Returns the maximum number of pending messages.
     *
     * @return The count limit.
     */
    public int getMaxPendingCount() {
        return maxPendingCount;
    }

    /**
     * Sets the maximum number of allowed pending messages.
     *
     * @param maxCount The count limit to set.
     * @return This, to allow chaining.
     */
    public MessageBusParams setMaxPendingCount(int maxCount) {
        this.maxPendingCount = maxCount;
        return this;
    }

    /**
     * Returns the maximum number of bytes allowed for pending messages.
     *
     * @return The size limit.
     */
    public int getMaxPendingSize() {
        return maxPendingSize;
    }

    /**
     * Sets the maximum number of bytes allowed for pending messages.
     *
     * @param maxSize The size limit to set.
     * @return This, to allow chaining.
     */
    public MessageBusParams setMaxPendingSize(int maxSize) {
        this.maxPendingSize = maxSize;
        return this;
    }
}
