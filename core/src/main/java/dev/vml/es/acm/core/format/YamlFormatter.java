package dev.vml.es.acm.core.format;

import dev.vml.es.acm.core.util.YamlUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class YamlFormatter {

    public <T> T read(InputStream inputStream, Class<T> clazz) throws IOException {
        return YamlUtils.read(inputStream, clazz);
    }

    public <T> T readFromString(String json, Class<T> clazz) throws IOException {
        return YamlUtils.readFromString(json, clazz);
    }

    public void write(OutputStream outputStream, Object data) throws IOException {
        YamlUtils.write(outputStream, data);
    }

    public String writeToString(Object data) throws IOException {
        return YamlUtils.writeToString(data);
    }
}
