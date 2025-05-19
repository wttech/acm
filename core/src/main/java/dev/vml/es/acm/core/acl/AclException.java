package dev.vml.es.acm.core.acl;

import dev.vml.es.acm.core.AcmException;

public class AclException extends AcmException {

    public AclException(String message) {
        super(message);
    }

    public AclException(String message, Throwable cause) {
        super(message, cause);
    }
}
