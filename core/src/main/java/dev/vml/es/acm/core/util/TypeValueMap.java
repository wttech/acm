package dev.vml.es.acm.core.util;

import java.util.Map;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * Works like Sling's value map but supports more types for conversion.
 * Added types: {@link java.time.LocalDateTime}, {@link java.time.LocalDate}, {@link java.time.LocalTime}.
 */
public class TypeValueMap extends ValueMapDecorator {

    public TypeValueMap(Map<String, Object> base) {
        super(base);
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return TypeUtils.convert(get(name), type, false).orElse(super.get(name, type));
    }
}
