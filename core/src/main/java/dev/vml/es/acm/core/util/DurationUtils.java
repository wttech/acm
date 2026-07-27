package dev.vml.es.acm.core.util;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class DurationUtils {

    private DurationUtils() {
        // intentionally empty
    }

    private static final Pattern HUMAN_PATTERN = Pattern.compile("(\\d+)\\s*(ms|s|m|h|d)", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a duration from a human-readable string (e.g. {@code '10m'}, {@code '2h30m'}, {@code '1d'},
     * {@code '500ms'}) or an ISO-8601 duration (e.g. {@code 'PT10M'}, {@code 'PT2H30M'}, {@code 'P1D'}).
     */
    public static Duration toDuration(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String normalized = text.trim();

        if (normalized.regionMatches(true, 0, "P", 0, 1)) {
            try {
                return Duration.parse(normalized);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(formatError(text), e);
            }
        }

        Matcher matcher = HUMAN_PATTERN.matcher(normalized);
        Duration result = Duration.ZERO;
        int matchedEnd = 0;
        boolean matched = false;
        while (matcher.find()) {
            if (matcher.start() != matchedEnd) {
                break; // gap means unexpected characters
            }
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(formatError(text), e);
            }
            result = result.plus(toUnitDuration(amount, matcher.group(2).toLowerCase()));
            matchedEnd = matcher.end();
            matched = true;
        }
        if (!matched || matchedEnd != normalized.length()) {
            throw new IllegalArgumentException(formatError(text));
        }
        return result;
    }

    private static Duration toUnitDuration(long amount, String unit) {
        switch (unit) {
            case "ms":
                return Duration.ofMillis(amount);
            case "s":
                return Duration.ofSeconds(amount);
            case "m":
                return Duration.ofMinutes(amount);
            case "h":
                return Duration.ofHours(amount);
            case "d":
                return Duration.ofDays(amount);
            default:
                throw new IllegalArgumentException(String.format("Unsupported duration unit '%s'!", unit));
        }
    }

    private static String formatError(String text) {
        return String.format("Cannot parse duration '%s'! Expected ISO-8601 (e.g. 'PT10M') or human-readable format (e.g. '10m', '2h30m', '1d', '500ms').", text);
    }

    /**
     * Converts milliseconds to a duration. A non-positive value yields a non-positive duration,
     * which downstream consumers (e.g. the locker) interpret as "no expiration".
     */
    public static Duration toDuration(long millis) {
        return Duration.ofMillis(millis);
    }

    public static Long toMillis(Duration duration) {
        return duration == null ? null : duration.toMillis();
    }
}
