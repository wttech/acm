package com.wttech.aem.acm.core.repository;

import com.wttech.aem.acm.core.AcmException;

public class RepositoryException extends AcmException {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
