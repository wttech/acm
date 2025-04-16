package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.AcmException;

public class RepositoryException extends AcmException {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
