package com.wttech.aem.contentor.core;

public class ContentorException extends RuntimeException {

    public ContentorException(String message) {
        super(message);
    }

    public ContentorException(Throwable cause) {
        super(cause);
    }

    public ContentorException(String message, Throwable cause) {
        super(message, cause);
    }
}
