package dev.vml.es.acm.core.format;

import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonFormatter {

    public <T> T read(InputStream inputStream, Class<T> clazz) throws IOException {
        return JsonUtils.read(inputStream, clazz);
    }

    public <T> T readFromString(String json, Class<T> clazz) throws IOException {
        return JsonUtils.readFromString(json, clazz);
    }

    public void write(OutputStream outputStream, Object data) throws IOException {
        JsonUtils.write(outputStream, data);
    }

    public String writeToString(Object data) throws IOException {
        return JsonUtils.writeToString(data);
    }
}
