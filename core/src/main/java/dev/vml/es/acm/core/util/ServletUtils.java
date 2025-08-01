package dev.vml.es.acm.core.util;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

public final class ServletUtils {

    private ServletUtils() {
        // intentionally empty
    }

    public static Integer intParam(SlingHttpServletRequest request, String name) {
        String value = stringParam(request, name);
        return value != null ? Integer.valueOf(value) : null;
    }

    public static Boolean boolParam(SlingHttpServletRequest request, String name) {
        String value = stringParam(request, name);
        return value != null ? Boolean.valueOf(value) : null;
    }

    public static Long longParam(SlingHttpServletRequest request, String name) {
        String value = stringParam(request, name);
        return value != null ? Long.valueOf(value) : null;
    }

    public static String stringParam(SlingHttpServletRequest request, String name) {
        return StringUtils.trimToNull(request.getParameter(name));
    }

    public static List<String> stringsParam(SlingHttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return null;
        }
        return Arrays.stream(values)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static void respondJson(SlingHttpServletResponse response, ServletResult<?> result) throws IOException {
        respondJsonBuffered(response, result);
    }

    /**
     * Responds with JSON result in streaming mode.
     * Consumes less memory, but JSON may be malformed in case of serialization errors.
     */
    public static void respondJsonStreamed(SlingHttpServletResponse response, ServletResult<?> result)
            throws IOException {
        response.setStatus(result.getStatus());
        response.setContentType(JsonUtils.APPLICATION_JSON_UTF8);

        JsonUtils.MAPPER.writeValue(response.getOutputStream(), result);
    }

    /**
     * Responds with JSON result in buffered mode.
     * Memory consumption is higher, but it allows to fail early in case of serialization errors.
     */
    public static void respondJsonBuffered(SlingHttpServletResponse response, ServletResult<?> result)
            throws IOException {
        response.setStatus(result.getStatus());
        response.setContentType(JsonUtils.APPLICATION_JSON_UTF8);

        String json = JsonUtils.MAPPER.writeValueAsString(result);
        response.getWriter().write(json);
    }
}
