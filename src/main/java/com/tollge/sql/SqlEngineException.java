package com.tollge.sql;

/**
 * 异常
 *
 * @author toyer
 * @since 2018-01-30
 */
public class SqlEngineException extends RuntimeException {
    public SqlEngineException(String s) {
        super(s);
    }

    public SqlEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
