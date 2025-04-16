package com.vml.es.aem.acm.core.script;

import com.vml.es.aem.acm.core.AcmException;

public class ScriptException extends AcmException {

    public ScriptException(String message) {
        super(message);
    }

    public ScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
