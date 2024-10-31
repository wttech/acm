package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;

public class ScriptException extends MigratorException {

    public ScriptException(String message) {
        super(message);
    }

    public ScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
