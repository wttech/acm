package com.wttech.aem.contentor.core.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

public final class YamlUtils {

    private static final Yaml YAML = new Yaml();

    static {
        YAML.setBeanAccess(BeanAccess.FIELD);
    }

    public static <T> T readYaml(InputStream inputStream, Class<T> clazz) {
        return YAML.loadAs(inputStream, clazz);
    }

    public static void writeYaml(OutputStream outputStream, Object data) {
        YAML.dump(data, new OutputStreamWriter(outputStream));
    }
}
