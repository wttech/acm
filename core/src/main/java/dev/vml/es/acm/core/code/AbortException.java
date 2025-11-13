package dev.vml.es.acm.core.code;

public class AbortException extends RuntimeException {

    public AbortException(String message) {
        super(message);
    }

    public AbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
