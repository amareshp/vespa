// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.grouping.request;

import java.util.Arrays;

/**
 * This class represents a day-of-week timestamp-function in a {@link GroupingExpression}. It evaluates to a long that
 * equals the day of week (0 - 6) of the result of the argument, Monday being 0.
 *
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class DayOfWeekFunction extends FunctionNode {

    /**
     * Constructs a new instance of this class.
     *
     * @param exp The expression to evaluate, must evaluate to a long.
     */
    public DayOfWeekFunction(GroupingExpression exp) {
        super("time.dayofweek", Arrays.asList(exp));
    }
}
