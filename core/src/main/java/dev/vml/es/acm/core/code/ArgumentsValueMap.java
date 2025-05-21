package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.util.DateUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class ArgumentsValueMap extends ValueMapDecorator {

    public ArgumentsValueMap(Map<String, Object> base) {
        super(base);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object value = get(name);

        if (value instanceof String) {
            if (type == LocalDateTime.class) {
                return (T) DateUtils.toLocalDateTime((String) value);
            } else if (type == LocalDate.class) {
                return (T) DateUtils.toLocalDate((String) value);
            } else if (type == LocalTime.class) {
                return (T) DateUtils.toLocalTime((String) value);
            }
        } else if (value instanceof Date) {
            if (type == LocalDateTime.class) {
                return (T) DateUtils.toLocalDateTime((Date) value);
            } else if (type == LocalDate.class) {
                return (T) DateUtils.toLocalDateTime((Date) value).toLocalDate();
            } else if (type == LocalTime.class) {
                return (T) DateUtils.toLocalDateTime((Date) value).toLocalTime();
            }
        } else if (value instanceof Calendar) {
            if (type == LocalDateTime.class) {
                return (T) DateUtils.toLocalDateTime((Calendar) value);
            } else if (type == LocalDate.class) {
                return (T) DateUtils.toLocalDateTime((Calendar) value).toLocalDate();
            } else if (type == LocalTime.class) {
                return (T) DateUtils.toLocalDateTime((Calendar) value).toLocalTime();
            }
        }

        return super.get(name, type);
    }
}
