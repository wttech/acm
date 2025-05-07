package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.util.DateUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class ValueMapDecoratorExtended extends ValueMapDecorator {
    /**
     * Creates a new wrapper around a given map.
     *
     * @param base wrapped object
     */
    public ValueMapDecoratorExtended(Map<String, Object> base) {
        super(base);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> type) {
        Object obj = get(name);
        // LocalDateTime conversion
        if (obj instanceof Date && type == LocalDateTime.class) {
            // Convert Date to LocalDateTime
            return (T) DateUtils.toLocalDateTime((Date) obj);
        } else if (obj instanceof Calendar && type == LocalDateTime.class) {
            // Convert Calendar to LocalDateTime
            return (T) DateUtils.toLocalDateTime((Calendar) obj);
        } else if (obj instanceof LocalDateTime && type == Date.class) {
            // Convert LocalDateTime to Date
            return (T) DateUtils.toDate((LocalDateTime) obj);
        } else if (obj instanceof LocalDateTime && type == Calendar.class) {
            // Convert LocalDateTime to Calendar
            return (T) DateUtils.toCalendar((LocalDateTime) obj);
        } else if (obj instanceof LocalDateTime && type == LocalDate.class) {
            // Convert LocalDateTime to LocalDate
            return (T) ((LocalDateTime) obj).toLocalDate();
        }

        // LocalDate conversion
        if (obj instanceof Date && type == LocalDate.class) {
            // Convert Date to LocalDate
            return (T) DateUtils.toLocalDateTime((Date) obj).toLocalDate();
        } else if (obj instanceof Calendar && type == LocalDate.class) {
            // Convert Calendar to LocalDate
            return (T) DateUtils.toLocalDateTime((Calendar) obj).toLocalDate();
        } else if (obj instanceof LocalDate && type == Date.class) {
            // Convert LocalDate to Date
            return (T) DateUtils.toDate(((LocalDate) obj).atTime(LocalTime.MIN));
        } else if (obj instanceof LocalDate && type == Calendar.class) {
            // Convert LocalDate to Calendar
            return (T) DateUtils.toCalendar(((LocalDate) obj).atTime(LocalTime.MIN));
        } else if (obj instanceof LocalDate && type == LocalDateTime.class) {
            // Convert LocalDate to LocalDateTime
            return (T) ((LocalDate) obj).atTime(LocalTime.MIN);
        }

        // String conversion
        if (obj instanceof String && type == LocalDateTime.class) {
            // Convert String to LocalDateTime
            return (T) DateUtils.toLocalDateTime((String) obj);
        } else if (obj instanceof String && type == Date.class) {
            // Convert String to Date
            return (T) DateUtils.fromString((String) obj);
        } else if (obj instanceof String && type == Calendar.class) {
            // Convert String to Calendar
            return (T) DateUtils.toCalendar((String) obj);
        } else if (obj instanceof String && type == LocalDate.class) {
            // Convert String to LocalDate
            return (T) DateUtils.toLocalDate((String) obj);
        }

        return super.get(name, type);
    }
}
