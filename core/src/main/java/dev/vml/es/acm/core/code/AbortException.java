package dev.vml.es.acm.core.code;

public class AbortException extends RuntimeException {

    private final AbortCause abortCause;

    public AbortException(String message) {
        super(message);
        this.abortCause = null;
    }

    public AbortException(String message, Throwable cause) {
        super(message, cause);
        this.abortCause = null;
    }

    public AbortException(AbortCause abortCause) {
        super(abortCause.getMessage());
        this.abortCause = abortCause;
    }

    public AbortCause getAbortCause() {
        return abortCause;
    }
}
