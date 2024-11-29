package com.wttech.aem.contentor.core.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class ServletUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ServletUtils.class);

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

            Object data = result.getData();
            JsonUtils.MAPPER.valueToTree(data).fields().forEachRemaining(entry -> {
                try {
                    generator.writeObjectField(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    LOG.error("Cannot serialize field: {}", entry.getKey(), e);
                }
            });
            if (data instanceof DataStreams) {
                for (DataStream<?> dataStream : ((DataStreams) data).dataStreams()) {
                    generator.writeArrayFieldStart(dataStream.name());
                    dataStream.items().forEach(item -> {
                        try {
                            generator.writeObject(item);
                        } catch (Exception e) {
                            LOG.error("Cannot serialize object: {}", item, e);
                        }
                    });
                    generator.writeEndArray();
                }
            }
            generator.writeEndObject();
        }
    }
}
