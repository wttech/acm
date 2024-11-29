package com.wttech.aem.contentor.core.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public final class ServletUtils {

    private ServletUtils() {
        // intentionally empty
    }

    public static String stringParam(SlingHttpServletRequest request, String name) {
        return StringUtils.trimToNull(request.getParameter(name));
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
            Object data = result.getData();
            Iterator<Map.Entry<String, JsonNode>> fields = JsonUtils.MAPPER.valueToTree(data).fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                generator.writeObjectField(entry.getKey(), entry.getValue());
            }
            if (data instanceof DataStreams) {
                for (DataStream<?> dataStream : ((DataStreams) data).dataStreams()) {
                    generator.writeArrayFieldStart(dataStream.name());
                    Iterator<?> iterator = dataStream.items().iterator();
                    while (iterator.hasNext()) {
                        Object item = iterator.next();
                        generator.writeObject(item);
                    }
                    generator.writeEndArray();
                }
            }
            generator.writeEndObject();

            generator.writeEndObject();
        }
    }
}
