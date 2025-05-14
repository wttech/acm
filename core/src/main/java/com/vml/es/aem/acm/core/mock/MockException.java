package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.AcmException;

public class MockException extends AcmException {

    public MockException(String message) {
        super(message);
    }

    public MockException(String message, Throwable cause) {
        super(message, cause);
    }
}
