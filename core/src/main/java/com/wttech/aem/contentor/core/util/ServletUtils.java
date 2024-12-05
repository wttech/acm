package com.wttech.aem.contentor.core.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
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

        try (JsonGenerator generator = new JsonFactory().createGenerator(response.getOutputStream(), JsonEncoding.UTF8)) {
            generator.setCodec(JsonUtils.MAPPER);
            generator.writeStartObject();

            generator.writeStringField("status", String.valueOf(result.getStatus()));
            generator.writeStringField("message", result.getMessage());

            generator.writeObjectFieldStart("data");
            generateData(generator, result);
            generator.writeEndObject();

            generator.writeEndObject();
        }
    }

    private static void generateData(JsonGenerator generator, ServletResult<?> result) throws IOException {
        Object data = result.getData();
        boolean dataWritten = false;
        Iterator<Map.Entry<String, JsonNode>> fields = JsonUtils.MAPPER.valueToTree(data).fields();

        while (fields.hasNext()) {
            dataWritten = true;
            Map.Entry<String, JsonNode> entry = fields.next();
            generator.writeObjectField(entry.getKey(), entry.getValue());
        }
        if (data instanceof DataStreams) {
            for (DataStream<?> dataStream : ((DataStreams) data).dataStreams()) {
                generator.writeArrayFieldStart(dataStream.name());
                Iterator<?> iterator = dataStream.items().iterator();
                while (iterator.hasNext()) {
                    dataWritten = true;
                    Object item = iterator.next();
                    generator.writeObject(item);
                }
                generator.writeEndArray();
            }
        }
        if (!dataWritten) {
            if (data instanceof Iterable) {
                generator.writeArrayFieldStart("values");
                for (Object item : (Iterable<?>) data) {
                    generator.writeObject(item);
                }
                generator.writeEndArray();
            } else if (data.getClass().isArray()) {
                generator.writeArrayFieldStart("values");
                int length = Array.getLength(data);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(data, i);
                    generator.writeObject(item);
                }
                generator.writeEndArray();
            } else {
                generator.writeObjectField("value", data);
            }
        }
    }
}
