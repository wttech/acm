package com.wttech.aem.migrator.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;

public final class JsonUtils {

    public static final String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
        // intentionally empty
    }

    public static void writeJson(Writer writer, Object data) throws IOException {
        OBJECT_MAPPER.writeValue(writer, data);
    }
}
