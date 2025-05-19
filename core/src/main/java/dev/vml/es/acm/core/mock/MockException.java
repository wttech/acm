package dev.vml.es.acm.core.mock;

import dev.vml.es.acm.core.AcmException;

public class MockException extends AcmException {

    public MockException(String message) {
        super(message);
    }

    public MockException(String message, Throwable cause) {
        super(message, cause);
    }
}
