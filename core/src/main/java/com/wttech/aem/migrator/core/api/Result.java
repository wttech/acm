package com.wttech.aem.migrator.core.api;

import javax.servlet.http.HttpServletResponse;
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

    public static Result notFound(String message) {
        return new Result(HttpServletResponse.SC_NOT_FOUND, message);
    }

    public static Result badRequest(String message) {
        return new Result(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    public static Result error(String message) {
        return new Result(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    public static Result ok(String message) {
        return new Result(HttpServletResponse.SC_OK, message);
    }

    public static Result ok(String message, Object data) {
        return new Result(HttpServletResponse.SC_OK, message, data);
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
