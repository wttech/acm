package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.util.DateUtils;
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

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object value = get(name);

        // LocalDateTime conversion
        if (value instanceof Date && type == LocalDateTime.class) {
            // Convert Date to LocalDateTime
            return (T) DateUtils.toLocalDateTime((Date) value);
        } else if (value instanceof Calendar && type == LocalDateTime.class) {
            // Convert Calendar to LocalDateTime
            return (T) DateUtils.toLocalDateTime((Calendar) value);
        } else if (value instanceof LocalDateTime && type == Date.class) {
            // Convert LocalDateTime to Date
            return (T) DateUtils.toDate((LocalDateTime) value);
        } else if (value instanceof LocalDateTime && type == Calendar.class) {
            // Convert LocalDateTime to Calendar
            return (T) DateUtils.toCalendar((LocalDateTime) value);
        } else if (value instanceof LocalDateTime && type == LocalDate.class) {
            // Convert LocalDateTime to LocalDate
            return (T) ((LocalDateTime) value).toLocalDate();
        }

        // LocalDate conversion
        if (value instanceof Date && type == LocalDate.class) {
            // Convert Date to LocalDate
            return (T) DateUtils.toLocalDateTime((Date) value).toLocalDate();
        } else if (value instanceof Calendar && type == LocalDate.class) {
            // Convert Calendar to LocalDate
            return (T) DateUtils.toLocalDateTime((Calendar) value).toLocalDate();
        } else if (value instanceof LocalDate && type == Date.class) {
            // Convert LocalDate to Date
            return (T) DateUtils.toDate(((LocalDate) value).atTime(LocalTime.MIN));
        } else if (value instanceof LocalDate && type == Calendar.class) {
            // Convert LocalDate to Calendar
            return (T) DateUtils.toCalendar(((LocalDate) value).atTime(LocalTime.MIN));
        } else if (value instanceof LocalDate && type == LocalDateTime.class) {
            // Convert LocalDate to LocalDateTime
            return (T) ((LocalDate) value).atTime(LocalTime.MIN);
        }

        // String conversion
        if (value instanceof String && type == LocalDateTime.class) {
            // Convert String to LocalDateTime
            return (T) DateUtils.toLocalDateTime((String) value);
        } else if (value instanceof String && type == Date.class) {
            // Convert String to Date
            return (T) DateUtils.fromString((String) value);
        } else if (value instanceof String && type == Calendar.class) {
            // Convert String to Calendar
            return (T) DateUtils.toCalendar((String) value);
        } else if (value instanceof String && type == LocalDate.class) {
            // Convert String to LocalDate
            return (T) DateUtils.toLocalDate((String) value);
        }

        return super.get(name, type);
    }
}
