package dev.vml.es.acm.core.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public final class TypeUtils {

    private TypeUtils() {
        // intentionally empty
    }

    /**
     * Convert a value to the specified type.
     * Fallback uses implicitly {@link org.apache.sling.api.wrappers.impl.ObjectConverter}.
     */
    public static <T> Optional<T> convert(Object value, Class<T> type, boolean fallback) {
        if (value instanceof String) {
            if (type == LocalDateTime.class) {
                return Optional.ofNullable((T) DateUtils.toLocalDateTime((String) value));
            } else if (type == LocalDate.class) {
                return Optional.ofNullable((T) DateUtils.toLocalDate((String) value));
            } else if (type == LocalTime.class) {
                return Optional.ofNullable((T) DateUtils.toLocalTime((String) value));
            }
        } else if (value instanceof Date) {
            if (type == LocalDateTime.class) {
                return Optional.ofNullable((T) DateUtils.toLocalDateTime((Date) value));
            } else if (type == LocalDate.class) {
                return Optional.ofNullable(
                        (T) DateUtils.toLocalDateTime((Date) value).toLocalDate());
            } else if (type == LocalTime.class) {
                return Optional.ofNullable(
                        (T) DateUtils.toLocalDateTime((Date) value).toLocalTime());
            }
        } else if (value instanceof Calendar) {
            if (type == LocalDateTime.class) {
                return Optional.ofNullable((T) DateUtils.toLocalDateTime((Calendar) value));
            } else if (type == LocalDate.class) {
                return Optional.ofNullable(
                        (T) DateUtils.toLocalDateTime((Calendar) value).toLocalDate());
            } else if (type == LocalTime.class) {
                return Optional.ofNullable(
                        (T) DateUtils.toLocalDateTime((Calendar) value).toLocalTime());
            }
        }

        if (fallback && value != null && !type.isAssignableFrom(value.getClass())) {
            T convertedValue = new ValueMapDecorator(Collections.singletonMap("v", value)).get("v", type);
            if (convertedValue != null) {
                return Optional.of(convertedValue);
            }
        }

        return Optional.empty();
    }
}
