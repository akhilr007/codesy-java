package com.codesy.platform.shared.exception;

public class ExecutionBackpressureException extends RuntimeException {

    public ExecutionBackpressureException(String message) {
        super(message);
    }
}