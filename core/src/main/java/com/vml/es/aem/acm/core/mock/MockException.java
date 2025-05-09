package com.vml.es.aem.acm.core.mock;

public class MockException extends Exception {

    public MockException(String message) {
        super(message);
    }

    public MockException(String message, Throwable cause) {
        super(message, cause);
    }
}