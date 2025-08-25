package dev.vml.es.acm.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * Works like Sling's value map but supports more types for conversion.
 * Added types: {@link java.time.LocalDateTime}, {@link java.time.LocalDate}, {@link java.time.LocalTime}.
 */
public class TypeValueMap extends ValueMapDecorator {

    public static final int ABBREVIATE_LENGTH = 256;

    public TypeValueMap(Map<String, Object> base) {
        super(base);
    }

    /**
     * Detach a map possibly containing session-backed data.
     * Materializes InputStream to byte[] and defensively copy mutable arrays.
     */
    public static TypeValueMap detached(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new TypeValueMap(Collections.emptyMap());
        }
        Map<String, Object> copy = new LinkedHashMap<>(source.size());
        for (Entry<String, Object> e : source.entrySet()) {
            Object v = e.getValue();
            Object detached = v;
            if (v instanceof InputStream) {
                try (InputStream in = (InputStream) v) {
                    detached = IOUtils.toByteArray(in);
                } catch (IOException io) {
                    throw new UncheckedIOException(
                            String.format("Failed to detach input stream for key '%s'!", e.getKey()), io);
                }
            } else if (v instanceof byte[]) {
                byte[] bytes = (byte[]) v;
                detached = bytes.clone();
            } else if (v instanceof char[]) {
                char[] chars = (char[]) v;
                detached = chars.clone();
            }
            copy.put(e.getKey(), detached);
        }
        return new TypeValueMap(Collections.unmodifiableMap(copy));
    }

    public Map<String, String> stringify() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : keySet()) {
            if (JcrConstants.JCR_DATA.equals(key)) {
                out.put(key, toStringJcrData());
            } else {
                out.put(key, toStringDefault(key));
            }
        }
        return out;
    }

    private String toStringDefault(String key) {
        return StringUtils.abbreviate(get(key, String.class), ABBREVIATE_LENGTH);
    }

    private String toStringJcrData() {
        Object v = get(JcrConstants.JCR_DATA);
        if (v instanceof byte[]) {
            return "{size=" + FileUtils.byteCountToDisplaySize(((byte[]) v).length) + "}";
        } else if (v instanceof InputStream) {
            try {
                int size = ((InputStream) v).available();
                return "{size=" + FileUtils.byteCountToDisplaySize(size) + "}";
            } catch (IOException ex) {
                return "{size=error}";
            }
        }
        return "{size=unknown}";
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return TypeUtils.convert(get(name), type, false).orElse(super.get(name, type));
    }
}
