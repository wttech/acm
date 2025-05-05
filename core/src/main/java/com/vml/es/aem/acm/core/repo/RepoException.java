package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.AcmException;

public class RepoException extends AcmException {

    public RepoException(String message) {
        super(message);
    }

    public RepoException(String message, Throwable cause) {
        super(message, cause);
    }
}
