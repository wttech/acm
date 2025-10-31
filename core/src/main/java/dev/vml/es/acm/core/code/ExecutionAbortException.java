package dev.vml.es.acm.core.code;


public class ExecutionAbortException extends RuntimeException {

    public ExecutionAbortException(String message) {
        super(message);
    }

    public ExecutionAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
