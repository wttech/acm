package com.wttech.aem.migrator.core.api;

import java.io.Serializable;

public class Result implements Serializable {

    private final int status;

    private final String message;

    private final Object data;

    public Result(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Result(int status, String message) {
        this(status, message, null);
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
