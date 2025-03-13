package com.wttech.aem.acm.core.acl;

import com.wttech.aem.acm.core.AcmException;

public class AclException extends AcmException {

    public AclException(String message) {
        super(message);
    }

    public AclException(String message, Throwable cause) {
        super(message, cause);
    }
}
