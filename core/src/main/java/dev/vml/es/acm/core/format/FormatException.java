package dev.vml.es.acm.core.format;

import dev.vml.es.acm.core.AcmException;

public class FormatException extends AcmException {

    public FormatException(String message) {
        super(message);
    }

    public FormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
