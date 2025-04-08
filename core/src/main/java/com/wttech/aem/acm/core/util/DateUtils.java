package com.wttech.aem.acm.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class DateUtils {

    private DateUtils() {
        // intentionally empty
    }

    private static SimpleDateFormat isoFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    private static SimpleDateFormat isoFormatNoMillis() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    private static SimpleDateFormat isoFormatNoMillisNoTimezone() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    private static Date fromStringInternal(String text) {
        for (SimpleDateFormat format : Arrays.asList(isoFormat(), isoFormatNoMillis(), isoFormatNoMillisNoTimezone())) {
            try {
                return format.parse(text);
            } catch (ParseException ignored) {
                // ignore
            }
        }
        throw new IllegalArgumentException(String.format("Cannot parse date '%s'!", text));
    }

    private static String toStringInternal(Date date) {
        return isoFormat().format(date);
    }

    public static String toString(Date date) {
        return Optional.ofNullable(date).map(DateUtils::toStringInternal).orElse(null);
    }

    public static Date fromString(String text) {
        return Optional.ofNullable(text).map(DateUtils::fromStringInternal).orElse(null);
    }

    public static Calendar toCalendar(Date date) {
        return Optional.ofNullable(date).map(DateUtils::toCalendarInternal).orElse(null);
    }

    private static Calendar toCalendarInternal(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    public static Date toDate(Calendar calendar) {
        return Optional.ofNullable(calendar).map(Calendar::getTime).orElse(null);
    }

    public static Calendar parseDate(Object date) {
        if (!(date instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(String.format("Cannot parse date '%s'!", date));
        }
        Map<String, Object> dateMap = (Map<String, Object>) date;
        // Check if all the keys are valid
        boolean isValidMap = ObjectUtils.validateStringObjectMap(dateMap);
        if (!isValidMap) {
            throw new IllegalArgumentException(String.format("Cannot parse date '%s'!", date));
        }
        Integer year = getIntegerFromMap(dateMap, "year");
        Integer month = getIntegerFromMap(dateMap, "month");
        Integer day = getIntegerFromMap(dateMap, "day");
        if (year == null || month == null || day == null) {
            throw new IllegalArgumentException(String.format("Cannot parse date '%s'!", date));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        return calendar;
    }

    private static Integer getIntegerFromMap(Map<String, Object> dateMap, String key) {
        Object value = dateMap.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            return null;
        }
        String stringVal = String.valueOf(value);
        return Integer.valueOf(stringVal);
    }
}
