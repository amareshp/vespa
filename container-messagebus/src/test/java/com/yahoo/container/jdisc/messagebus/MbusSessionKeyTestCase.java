// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.jdisc.messagebus;

import com.yahoo.container.jdisc.messagebus.SessionCache.DynamicThrottlePolicySignature;
import com.yahoo.container.jdisc.messagebus.SessionCache.SourceSessionKey;
import com.yahoo.container.jdisc.messagebus.SessionCache.StaticThrottlePolicySignature;
import com.yahoo.container.jdisc.messagebus.SessionCache.UnknownThrottlePolicySignature;
import com.yahoo.messagebus.DynamicThrottlePolicy;
import com.yahoo.messagebus.SourceSessionParams;
import com.yahoo.messagebus.StaticThrottlePolicy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Check the completeness of the mbus session key classes.
 *
 * @author <a href="mailto:steinar@yahoo-inc.com">Steinar Knutsen</a>
 */
public class MbusSessionKeyTestCase {

    @Test
    public final void staticThrottlePolicySignature() {
        final StaticThrottlePolicy base = new StaticThrottlePolicy();
        final StaticThrottlePolicy other = new StaticThrottlePolicy();
        other.setMaxPendingCount(500);
        other.setMaxPendingSize(500 * 1000 * 1000);
        base.setMaxPendingCount(1);
        base.setMaxPendingSize(1000);
        final StaticThrottlePolicySignature sigBase = new StaticThrottlePolicySignature(
                base);
        final StaticThrottlePolicySignature sigOther = new StaticThrottlePolicySignature(
                other);
        assertFalse("The policies are different, but signatures are equal.",
                sigBase.equals(sigOther));
        assertTrue("Sigs created from same policy evaluated as different.",
                sigBase.equals(new StaticThrottlePolicySignature(base)));
        other.setMaxPendingCount(1);
        other.setMaxPendingSize(1000);
        assertTrue(
                "Sigs created from different policies with same settings evaluated as different.",
                sigBase.equals(new StaticThrottlePolicySignature(other)));

    }

    @Test
    public final void dynamicThrottlePolicySignature() {
        final DynamicThrottlePolicy base = new DynamicThrottlePolicy();
        final DynamicThrottlePolicy other = new DynamicThrottlePolicy();
        base.setEfficiencyThreshold(5);
        base.setMaxPendingCount(3);
        base.setMaxPendingSize(3 * 100);
        base.setMaxThroughput(1e6);
        base.setMaxWindowSize(1e9);
        base.setMinWindowSize(1e5);
        base.setWeight(1.0);
        base.setWindowSizeBackOff(.6);
        base.setWindowSizeIncrement(500);
        other.setEfficiencyThreshold(5 + 1);
        other.setMaxPendingCount(3 + 1);
        other.setMaxPendingSize(3 * 100 + 1);
        other.setMaxThroughput(1e6 + 1);
        other.setMaxWindowSize(1e9 + 1);
        other.setMinWindowSize(1e5 + 1);
        other.setWeight(1.0 + 1);
        other.setWindowSizeBackOff(.6 + 1);
        other.setWindowSizeIncrement(500 + 1);
        final DynamicThrottlePolicySignature sigBase = new DynamicThrottlePolicySignature(
                base);
        final DynamicThrottlePolicySignature sigOther = new DynamicThrottlePolicySignature(
                other);
        assertFalse("The policies are different, but signatures are equal.",
                sigBase.equals(sigOther));
        assertTrue("Sigs created from same policy evaluated as different.",
                sigBase.equals(new DynamicThrottlePolicySignature(base)));
        other.setEfficiencyThreshold(5);
        other.setMaxPendingCount(3);
        other.setMaxPendingSize(3 * 100);
        other.setMaxThroughput(1e6);
        other.setMaxWindowSize(1e9);
        other.setMinWindowSize(1e5);
        other.setWeight(1.0);
        other.setWindowSizeBackOff(.6);
        other.setWindowSizeIncrement(500);
        assertTrue(
                "Sigs created from different policies with same settings evaluated as different.",
                sigBase.equals(new DynamicThrottlePolicySignature(other)));
    }

    @Test
    public final void unknownThrottlePolicySignature() {
        final UnknownThrottlePolicySignature baseSig = new UnknownThrottlePolicySignature(
                new StaticThrottlePolicy());
        final UnknownThrottlePolicySignature otherSig = new UnknownThrottlePolicySignature(
                new StaticThrottlePolicy());
        assertEquals(baseSig, baseSig);
        assertFalse(otherSig.equals(baseSig));
    }

    // TODO the session key tests are just smoke tests

    @Test
    public final void sourceSessionKey() {
        final SourceSessionParams base = new SourceSessionParams();
        final SourceSessionParams other = new SourceSessionParams();
        assertEquals(new SourceSessionKey(base), new SourceSessionKey(other));
        other.setTimeout(other.getTimeout() + 1);
        assertFalse(new SourceSessionKey(base).equals(new SourceSessionKey(
                other)));
    }
}
