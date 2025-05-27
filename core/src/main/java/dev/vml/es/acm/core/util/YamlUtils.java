package dev.vml.es.acm.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class YamlUtils {

    public static final String MIME_TYPE = "application/x-yaml";

    public static final String EXTENSION = "yml";

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private YamlUtils() {
        // intentionally empty
    }

    public static <T> T read(InputStream inputStream, Class<T> clazz) throws IOException {
        return YAML_MAPPER.readValue(inputStream, clazz);
    }

    public static void write(OutputStream outputStream, Object data) throws IOException {
        YAML_MAPPER.writeValue(outputStream, data);
    }

    public static <T> T readFromString(String yaml, Class<T> clazz) throws IOException {
        return YAML_MAPPER.readValue(yaml, clazz);
    }

    public static String writeToString(Object data) throws IOException {
        return YAML_MAPPER.writeValueAsString(data);
    }
}
