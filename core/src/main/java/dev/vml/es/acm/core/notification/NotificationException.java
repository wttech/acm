package dev.vml.es.acm.core.notification;

import dev.vml.es.acm.core.AcmException;

public class NotificationException extends AcmException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
