package com.codesy.platform.execution.exception;

public class SandboxExecutionException extends RuntimeException {
    public SandboxExecutionException(String message) {
        super(message);
    }

    public SandboxExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}