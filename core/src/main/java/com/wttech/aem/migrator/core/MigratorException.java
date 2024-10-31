package com.wttech.aem.migrator.core;

public class MigratorException extends Exception {

    public MigratorException(String message) {
        super(message);
    }

    public MigratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
