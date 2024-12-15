package com.wttech.aem.contentor.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

public final class DateUtils {

    private DateUtils() {
        // intentionally empty
    }

    private static SimpleDateFormat isoFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    private static Date fromStringInternal(String text) {
        try {
            return isoFormat().parse(text);
        } catch (ParseException e) {
            return null;
        }
    }

    private static String toStringInternal(Date date) {
        return isoFormat().format(date);
    }

    public static String toString(Date date) {
        return Optional.ofNullable(date)
                .map(DateUtils::toStringInternal)
                .orElse(null);
    }

    public static Date fromString(String text) {
        return Optional.ofNullable(text)
                .map(DateUtils::fromStringInternal)
                .orElse(null);
    }

    public static Calendar toCalendar(Date date) {
        return Optional.ofNullable(date)
                .map(DateUtils::toCalendarInternal)
                .orElse(null);
    }

    private static Calendar toCalendarInternal(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    public static Date toDate(Calendar calendar) {
        return Optional.ofNullable(calendar)
                .map(Calendar::getTime)
                .orElse(null);
    }
}
