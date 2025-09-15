package dev.vml.es.acm.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class DateUtils {

    private DateUtils() {
        // intentionally empty
    }

    public static final String TIMEZONE_ID = TimeZone.getDefault().getID();

    public static final ZoneId ZONE_ID = ZoneId.of(DateUtils.TIMEZONE_ID);

    private static final List<String> LOCAL_DATE_TIME_FORMATS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX");

    public static SimpleDateFormat humanFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    public static SimpleDateFormat isoFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    public static SimpleDateFormat isoFormatNoMillis() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    public static SimpleDateFormat isoFormatNoMillisNoTimezone() {
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
        throw new IllegalArgumentException(String.format("Cannot parse zoned datetime '%s'!", text));
    }

    public static LocalDateTime toLocalDateTime(String text) {
        for (String format : LOCAL_DATE_TIME_FORMATS) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.of(TIMEZONE_ID));
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // ignore
            }
        }
        throw new IllegalArgumentException(String.format("Cannot parse datetime '%s'!", text));
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

    public static Calendar toCalendar(String date) {
        return Optional.ofNullable(date)
                .map(d -> {
                    Date parsedDate = fromString(d);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(parsedDate);
                    return calendar;
                })
                .orElse(null);
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

    public static LocalDate toLocalDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(String.format("Cannot parse date '%s'!", text), e);
        }
    }

    public static LocalTime toLocalTime(String text) {
        try {
            return LocalTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(String.format("Cannot parse time '%s'!", text), e);
        }
    }
}
