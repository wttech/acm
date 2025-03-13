package com.wttech.aem.acm.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
}
