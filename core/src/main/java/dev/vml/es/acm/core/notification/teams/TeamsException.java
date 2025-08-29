package dev.vml.es.acm.core.notification.teams;

import dev.vml.es.acm.core.AcmException;

public class TeamsException extends AcmException {

    public TeamsException(String message) {
        super(message);
    }

    public TeamsException(String message, Throwable cause) {
        super(message, cause);
    }
}
