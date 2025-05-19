package dev.vml.es.acm.core.mock;

public class MockResponseException extends MockException {

    public MockResponseException(String message, Exception e) {
        super(message, e);
    }

    public MockResponseException(String message) {
        super(message);
    }
}
