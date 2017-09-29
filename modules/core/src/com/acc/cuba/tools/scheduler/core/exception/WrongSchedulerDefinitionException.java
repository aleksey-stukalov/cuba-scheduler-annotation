package com.acc.cuba.tools.scheduler.core.exception;

/**
 * Created by Aleksey Stukalov on 27/09/2017.
 */
public class WrongSchedulerDefinitionException extends RuntimeException {

    public WrongSchedulerDefinitionException(String message) {
        super(message);
    }

    public WrongSchedulerDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

}
