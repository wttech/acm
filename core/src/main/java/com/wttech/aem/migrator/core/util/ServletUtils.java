package com.wttech.aem.migrator.core.util;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

public final class ServletUtils {

    private ServletUtils() {
        // intentionally empty
    }

    public static String stringParam(SlingHttpServletRequest request, String name) {
        return StringUtils.trimToNull(request.getParameter(name));
    }

    public static void respondJson(SlingHttpServletResponse response, Object data) throws IOException {
        response.setContentType(JsonUtils.APPLICATION_JSON_UTF8);
        JsonUtils.writeJson(response.getWriter(), data);
    }
}
