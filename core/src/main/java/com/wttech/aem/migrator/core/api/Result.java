package com.wttech.aem.migrator.core.api;

import java.io.Serializable;

public class Result implements Serializable {

    private final int status;

    private final String message;

    public Result(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
