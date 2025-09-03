package dev.vml.es.acm.core.notification;

import dev.vml.es.acm.core.AcmException;

public class NotifierException extends AcmException{

    public NotifierException(String message) {
        super(message);
    }

    public NotifierException(String message, Throwable cause) {
        super(message, cause);
    }

}
