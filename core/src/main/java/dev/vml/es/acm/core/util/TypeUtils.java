package dev.vml.es.acm.core.util;

import java.io.File;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public final class TypeUtils {

    private TypeUtils() {
        // intentionally empty
    }

    /**
     * Convert a value to the specified type.
     * Fallback uses implicitly {@link org.apache.sling.api.wrappers.impl.ObjectConverter}.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> convert(Object value, Class<T> type, boolean fallback) {
        if (type.isArray()) {
            Object array = convertToArray(value, type.getComponentType(), fallback);
            return Optional.ofNullable((T) array);
        }

        if (value instanceof String) {
            if (type == LocalDateTime.class) {
                return Optional.ofNullable((T) DateUtils.toLocalDateTime((String) value));
            } else if (type == LocalDate.class) {
                return Optional.ofNullable((T) DateUtils.toLocalDate((String) value));
            } else if (type == LocalTime.class) {
                return Optional.ofNullable((T) DateUtils.toLocalTime((String) value));
            } else if (type == File.class) {
                return Optional.ofNullable((T) new File((String) value));
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

    @SuppressWarnings("unchecked")
    private static <T> T[] convertToArray(Object obj, Class<T> type, boolean fallback) {
        if (obj == null) {
            return (T[]) Array.newInstance(type, 0);
        }
        if (obj.getClass().isArray()) {
            List<T> resultList = new ArrayList<>();
            for (int i = 0; i < Array.getLength(obj); ++i) {
                Optional<T> convertedValue = convert(Array.get(obj, i), type, fallback);
                convertedValue.ifPresent(resultList::add);
            }
            return resultList.toArray((T[]) Array.newInstance(type, resultList.size()));
        } else if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            List<T> resultList = new ArrayList<>(collection.size());
            for (Object element : collection) {
                Optional<T> convertedValue = convert(element, type, fallback);
                convertedValue.ifPresent(resultList::add);
            }
            return resultList.toArray((T[]) Array.newInstance(type, resultList.size()));
        } else {
            Optional<T> convertedValue = convert(obj, type, fallback);
            if (!convertedValue.isPresent()) {
                return (T[]) Array.newInstance(type, 0);
            } else {
                T[] arrayResult = (T[]) Array.newInstance(type, 1);
                arrayResult[0] = convertedValue.get();
                return arrayResult;
            }
        }
    }
}
