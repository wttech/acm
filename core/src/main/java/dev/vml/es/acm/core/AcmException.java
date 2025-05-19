package dev.vml.es.acm.core;

public class AcmException extends RuntimeException {

    public AcmException(String message) {
        super(message);
    }

    public AcmException(String message, Throwable cause) {
        super(message, cause);
    }
}
