package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.util.DateUtils;
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
    public <T> T get(String name, Class<T> type) {
        Object obj = get(name);
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
        } else {
            return super.get(name, type);
        }
    }
}
