package dev.vml.es.acm.core.notification.slack;

import dev.vml.es.acm.core.AcmException;

public class SlackException extends AcmException {

    public SlackException(String message) {
        super(message);
    }

    public SlackException(String message, Throwable cause) {
        super(message, cause);
    }
}
