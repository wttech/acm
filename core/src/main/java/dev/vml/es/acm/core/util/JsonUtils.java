package dev.vml.es.acm.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public final class JsonUtils {

    public static final String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule());

    public static final ObjectWriter COMPACT_WRITER = MAPPER.writer();

    // public static final ObjectWriter PRETTY_WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    private JsonUtils() {
        // intentionally empty
    }

    public static <T> T read(InputStream inputStream, Class<T> clazz) throws IOException {
        return MAPPER.readValue(inputStream, clazz);
    }

    public static <T> T readFromString(String json, Class<T> clazz) throws IOException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return MAPPER.readValue(json, clazz);
    }

    public static void write(OutputStream outputStream, Object data) throws IOException {
        COMPACT_WRITER.writeValue(outputStream, data);
    }

    public static String writeToString(Object data) throws IOException {
        return COMPACT_WRITER.writeValueAsString(data);
    }

    public static InputStream writeToStream(Object data) throws IOException {
        return new ByteArrayInputStream(COMPACT_WRITER.writeValueAsBytes(data));
    }

    public static String mapToString(Map<String, Object> object) throws IOException {
        if (object == null) {
            return "{}";
        }
        return MAPPER.writeValueAsString(object);
    }

    public static Map<String, Object> stringToMap(String json) throws IOException {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyMap();
        }
        return MAPPER.readValue(json, MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }
}
