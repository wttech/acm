package dev.vml.es.acm.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import javax.jcr.Node;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ValueMap;
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

    public static TypeValueMap detached(Map<String, Object> base) {
        return new TypeValueMap(detach(base));
    }

    /**
     * Detach a map possibly containing session-backed data.
     * Materializes InputStream to byte[] and defensively copy mutable arrays.
     */
    public static Map<String, Object> detach(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<String, Object> e : source.entrySet()) {
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

    public static Map<String, String> toString(ValueMap vm, Node node) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : vm.keySet()) {
            if (JcrConstants.JCR_DATA.equals(key)) {
                out.put(key, toStringJcrData(vm, node));
            } else {
                out.put(key, toStringDefault(vm, key));
            }
        }
        return out;
    }

    private static String toStringDefault(ValueMap vm, String key) {
        return StringUtils.abbreviate(vm.get(key, String.class), ABBREVIATE_LENGTH);
    }

    private static String toStringJcrData(ValueMap vm, Node node) {
        Object v = vm.get(JcrConstants.JCR_DATA);
        if (node != null) {
            try {
                long size = node.getProperty(JcrConstants.JCR_DATA).getLength();
                return JcrConstants.JCR_DATA + "[size=" + FileUtils.byteCountToDisplaySize(size) + "]";
            } catch (Exception ex) {
                // ignore
            }
        }
        if (v instanceof byte[]) {
            return JcrConstants.JCR_DATA + "[size=" + FileUtils.byteCountToDisplaySize(((byte[]) v).length) + "]";
        } else if (v instanceof InputStream) {
            try {
                int size = ((InputStream) v).available();
                return JcrConstants.JCR_DATA + "[size=" + FileUtils.byteCountToDisplaySize(size) + "]";
            } catch (IOException ex) {
                return JcrConstants.JCR_DATA + "[size=error]";
            }
        }
        return JcrConstants.JCR_DATA + "[size=unknown]";
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return TypeUtils.convert(get(name), type, false).orElse(super.get(name, type));
    }
}
