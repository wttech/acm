package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.AcmException;

public class RepoException extends AcmException {

    public RepoException(String message) {
        super(message);
    }

    public RepoException(String message, Throwable cause) {
        super(message, cause);
    }
}
