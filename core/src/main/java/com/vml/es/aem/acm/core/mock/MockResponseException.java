package com.vml.es.aem.acm.core.mock;

public class MockResponseException extends MockException {

    public MockResponseException(String message, Exception e) {
        super(message, e);
    }

    public MockResponseException(String message) {
        super(message);
    }
}