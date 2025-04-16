package com.vml.es.aem.acm.core.acl;

import com.vml.es.aem.acm.core.AcmException;

public class AclException extends AcmException {

    public AclException(String message) {
        super(message);
    }

    public AclException(String message, Throwable cause) {
        super(message, cause);
    }
}
