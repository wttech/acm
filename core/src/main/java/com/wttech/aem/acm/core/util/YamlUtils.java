package com.wttech.aem.acm.core.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlUtils {

    private static final Yaml YAML;

    static {
        YAML = customYaml();
    }

    private static Yaml customYaml() {
        Constructor constructor = new Constructor();
        constructor.getPropertyUtils().setSkipMissingProperties(true);
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer();
        Yaml yaml = new Yaml(constructor, representer, options);
        yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml;
    }

    public static <T> T readYaml(InputStream inputStream, Class<T> clazz) {
        return YAML.loadAs(new BufferedInputStream(inputStream), clazz);
    }

    public static void writeYaml(OutputStream outputStream, Object data) {
        YAML.dump(data, new OutputStreamWriter(outputStream));
    }
}
