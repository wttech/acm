package com.wttech.aem.contentor.core.acl;

import com.wttech.aem.contentor.core.ContentorException;

public class AclException extends ContentorException {

    public AclException(String message) {
        super(message);
    }

    public AclException(Throwable cause) {
        super(cause);
    }

    public AclException(String message, Throwable cause) {
        super(message, cause);
    }
}
