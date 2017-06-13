// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.config.codegen;

/**
 * This exception is thrown on internal errors.
 *
 * @author <a href="gv@yahoo-inc.com">G. Voldengen</a>
 */
public class CodegenRuntimeException extends RuntimeException {
    public CodegenRuntimeException(String s, Throwable cause) {
        super(s, cause);
    }

    public CodegenRuntimeException(Throwable cause) {
        super(cause);
    }

    public CodegenRuntimeException(String s) {
        super(s);
    }
}
