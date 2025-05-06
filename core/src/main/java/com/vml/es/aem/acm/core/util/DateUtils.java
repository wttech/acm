package com.vml.es.aem.acm.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class DateUtils {

    private DateUtils() {
        // intentionally empty
    }

    public static final String TIMEZONE_ID = TimeZone.getDefault().getID();

    public static final ZoneId ZONE_ID = ZoneId.of(DateUtils.TIMEZONE_ID);

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

    public static ZonedDateTime zonedDateTimeFromString(String text) {
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

    public static LocalDateTime localDateTimeFromString(String text) {
        for (String format : LOCAL_DATE_TIME_FORMATS) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.of(TIMEZONE_ID));
            try {
                return LocalDateTime.parse(text, formatter);
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

    public static Date toDate(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(ldt -> Date.from(ldt.atZone(ZONE_ID).toInstant()))
                .orElse(null);
    }

    public static Calendar toCalendar(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(ldt -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(Date.from(ldt.atZone(ZONE_ID).toInstant()));
                    return calendar;
                })
                .orElse(null);
    }

    public static boolean isInRange(LocalDateTime from, LocalDateTime now, long offsetMillis) {
        LocalDateTime to = from.plus(offsetMillis, ChronoUnit.MILLIS);
        return !now.isBefore(from) && !now.isAfter(to);
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return Optional.ofNullable(date)
                .map(d -> d.toInstant().atZone(ZONE_ID).toLocalDateTime())
                .orElse(null);
    }

    public static LocalDateTime toLocalDateTime(Calendar calendar) {
        return Optional.ofNullable(calendar)
                .map(c -> c.toInstant().atZone(ZONE_ID).toLocalDateTime())
                .orElse(null);
    }
}
