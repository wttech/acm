package com.wttech.aem.migrator.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class JsonUtils {

    public static final String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static final ObjectWriter COMPACT_WRITER = MAPPER.writer();

    // public static final ObjectWriter PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private JsonUtils() {
        // intentionally empty
    }

    public static <T> T readJson(InputStream inputStream, Class<T> clazz) throws IOException {
        return MAPPER.readValue(inputStream, clazz);
    }

    public static void writeJson(OutputStream outputStream, Object data) throws IOException {
        COMPACT_WRITER.writeValue(outputStream, data);
    }
}
