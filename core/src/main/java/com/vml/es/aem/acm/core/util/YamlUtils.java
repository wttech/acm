package com.vml.es.aem.acm.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class YamlUtils {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private YamlUtils() {
        // intentionally empty
    }

    public static <T> T readYaml(InputStream inputStream, Class<T> clazz) throws IOException {
        return YAML_MAPPER.readValue(inputStream, clazz);
    }

    public static void writeYaml(OutputStream outputStream, Object data) throws IOException {
        YAML_MAPPER.writeValue(outputStream, data);
    }
}
