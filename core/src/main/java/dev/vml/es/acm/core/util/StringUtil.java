package dev.vml.es.acm.core.util;

import java.util.Arrays;

import org.apache.commons.io.FileUtils;
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

    private static String truncateCode(
            String code, int maxLength, boolean skipStart, String emptyMessage) {
        if (code == null || code.trim().isEmpty()) {
            return emptyMessage;
        }
        String text = StringUtils.trimToEmpty(code);
        if (maxLength < 0 || text.length() <= maxLength) {
            return "```\n" + text + "\n```";
        }
        int skippedChars = text.length() - maxLength;
        String remaining;
        int skippedLines;
        
        if (skipStart) {
            remaining = text.substring(skippedChars);
            String skippedPart = text.substring(0, skippedChars);
            skippedLines = skippedPart.isEmpty() ? 0 : skippedPart.split("\r?\n").length;
        } else {
            remaining = text.substring(0, maxLength);
            String skippedPart = text.substring(maxLength);
            skippedLines = skippedPart.isEmpty() ? 0 : skippedPart.split("\r?\n").length;
        }
        String humanSize = FileUtils.byteCountToDisplaySize(code.length());
        String lineWord = skippedLines == 1 ? "line" : "lines";
        String msg;
        if (skippedLines > 0) {
            msg = String.format("(%s, %d %s skipped)", humanSize, skippedLines, lineWord);
        } else {
            msg = String.format("(%s)", humanSize);
        }
        return msg + "\n```\n" + remaining + "\n```";
    }

    public static String truncateCodeStart(String code, int maxLength) {
        return truncateCode(code, maxLength, true, "(empty)");
    }

    public static String truncateCodeEnd(String text, int maxLength) {
        return truncateCode(text, maxLength, false, "(empty)");
    }

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0)
            sb.append(hours).append(" hour").append(hours > 1 ? "s " : " ");
        if (minutes > 0)
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s " : " ");
        if (secs > 0)
            sb.append(secs).append(" second").append(secs != 1 ? "s" : "");

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
