package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmException;

public class ScriptException extends AcmException {

    public ScriptException(String message) {
        super(message);
    }

    public ScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
