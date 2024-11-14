package com.wttech.aem.migrator.core.util;

import com.wttech.aem.migrator.core.api.Result;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;

public final class ServletUtils {

    private ServletUtils() {
        // intentionally empty
    }

    public static String stringParam(SlingHttpServletRequest request, String name) {
        return StringUtils.trimToNull(request.getParameter(name));
    }

    public static void respondJson(SlingHttpServletResponse response, Result result) throws IOException {
        response.setStatus(result.getStatus());
        respondJson(response, (Object) result);
    }

    public static void respondJson(SlingHttpServletResponse response, Object data) throws IOException {
        if (data instanceof Result) {
            response.setStatus(((Result) data).getStatus());
        }
        response.setContentType(JsonUtils.APPLICATION_JSON_UTF8);
        JsonUtils.writeJson(response.getOutputStream(), data);
    }
}
