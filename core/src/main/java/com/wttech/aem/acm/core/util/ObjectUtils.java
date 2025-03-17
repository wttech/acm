package com.wttech.aem.acm.core.util;

public final class ObjectUtils {

    private ObjectUtils() {
        // intentionally empty
    }

    public static boolean toBool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else {
            throw new IllegalArgumentException("Cannot convert object to boolean: " + value);
        }
    }

    public static long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        } else {
            throw new IllegalArgumentException("Cannot convert object to long: " + value);
        }
    }
}
