package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.util.DateUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        Object obj = get(name);if (obj instanceof String && type == LocalDateTime.class) {
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
        } else {
            return super.get(name, type);
        }
    }
}
