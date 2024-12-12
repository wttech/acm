package com.wttech.aem.contentor.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ServletUtils {

    private ServletUtils() {
        // intentionally empty
    }

    public static String stringParam(SlingHttpServletRequest request, String name) {
        return StringUtils.trimToNull(request.getParameter(name));
    }

    public static List<String> stringsParam(SlingHttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(values)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static void respondJson(SlingHttpServletResponse response, ServletResult<?> result) throws IOException {
        response.setStatus(result.getStatus());
        response.setContentType(JsonUtils.APPLICATION_JSON_UTF8);

        JsonUtils.MAPPER.writeValue(response.getOutputStream(), result);
    }
}
