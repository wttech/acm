package dev.vml.es.acm.core.util;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public final class StringUtil {

    private StringUtil() {
        // intentionally empty
    }

    public static String capitalizeWords(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        // Insert spaces before capital letters
        text = text.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        // Capitalize each word
        return Arrays.stream(StringUtils.split(text, " "))
                .map(StringUtils::capitalize)
                .reduce((a, b) -> a + " " + b)
                .orElse(text);
    }

    public static String toStringOrEmpty(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    public static String abbreviateStart(String str, int maxLength) {
        return abbreviateStart(str, maxLength, "...");
    }

    public static String abbreviateStart(String str, int maxLength, String prefix) {
        if (str == null || maxLength < 0) {
            return str;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        if (maxLength <= prefix.length()) {
            return prefix.substring(0, maxLength);
        }
        return prefix + StringUtils.right(str, maxLength - prefix.length());
    }

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(" hour").append(hours > 1 ? "s " : " ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes > 1 ? "s " : " ");
        if (secs > 0) sb.append(secs).append(" second").append(secs != 1 ? "s" : "");

        String humanReadable = sb.toString().trim();
        if (humanReadable.isEmpty()) {
            return millis + " ms";
        } else {
            return millis + " ms (" + humanReadable + ")";
        }
    }

    public static String markdownCode(String code, String defaultString) {
        if (StringUtils.isBlank(code)) {
            return defaultString;
        }
        return "```\n" + code.trim() + "\n```";
    }
}
