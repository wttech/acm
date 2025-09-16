package dev.vml.es.acm.core.util;

import java.io.Serializable;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

public class ServletResult<D extends Serializable> implements Serializable {

    private final int status;

    private final String message;

    private final D data;

    public ServletResult(int status, String message, D data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ServletResult(int status, String message) {
        this(status, trimMessage(message), null);
    }

    private static String trimMessage(String message) {
        return StringUtils.trim(StringUtils.removeEnd(message, "null"));
    }

    public static ServletResult<Void> notFound(String message) {
        return new ServletResult<>(HttpServletResponse.SC_NOT_FOUND, message);
    }

    public static ServletResult<Void> badRequest(String message) {
        return new ServletResult<>(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    public static ServletResult<Void> error(String message) {
        return new ServletResult<>(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    public static ServletResult<Void> ok(String message) {
        return new ServletResult<>(HttpServletResponse.SC_OK, message);
    }

    public static <D> ServletResult<D> ok(String message, D data) {
        return new ServletResult<>(HttpServletResponse.SC_OK, message, data);
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public D getData() {
        return data;
    }
}
