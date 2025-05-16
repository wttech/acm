package com.vml.es.aem.acm.core.format;

import com.vml.es.aem.acm.core.AcmException;

public class FormatException extends AcmException {

    public FormatException(String message) {
        super(message);
    }

    public FormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
