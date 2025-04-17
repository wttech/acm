package com.vml.es.aem.acm.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class DateUtils {

    private DateUtils() {
        // intentionally empty
    }

    public static final String TIMEZONE_ID = TimeZone.getDefault().getID();

    private static final List<String> LOCAL_DATE_TIME_FORMATS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX");

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

    public static ZonedDateTime localDateTimeFromString(String text) {
        for (String format : LOCAL_DATE_TIME_FORMATS) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.of(TIMEZONE_ID));
            try {
                return ZonedDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // ignore
            }
        }
        throw new IllegalArgumentException(String.format("Cannot parse date '%s'!", text));
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

    public static boolean isInRange(LocalDateTime from, LocalDateTime now, long offset) {
        // Multiplying by 1_000_000 to convert milliseconds to nanoseconds
        LocalDateTime to = from.plusNanos(offset * 1_000_000);
        return !now.isBefore(from) && !now.isAfter(to);
    }
}
