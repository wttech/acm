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

    private static String truncateCode(String code, int maxLength, boolean skipStart, String emptyMessage) {
        if (code == null || code.trim().isEmpty()) {
            return emptyMessage;
        }
        String humanSize = FileUtils.byteCountToDisplaySize(code.length());
        String text = StringUtils.trimToEmpty(code);
        
        int totalLines = text.isEmpty() ? 0 : text.split("\r?\n").length;
        String lineWord = totalLines == 1 ? "line" : "lines";
        
        if (maxLength < 0 || text.length() <= maxLength) {
            String msg = String.format("%s, %d %s", humanSize, totalLines, lineWord);
            return String.format("%s\n```\n%s\n```", msg, text);
        }
        
        String remaining;
        String rangeInfo;
        if (skipStart) {
            int skippedChars = text.length() - maxLength;
            remaining = text.substring(skippedChars);
            rangeInfo = String.format("last %d chars", maxLength);
        } else {
            remaining = text.substring(0, maxLength);
            rangeInfo = String.format("first %d chars", maxLength);
        }
        
        String msg = String.format("%s, %d %s (%s)", humanSize, totalLines, lineWord, rangeInfo);
        return String.format("%s\n```\n%s\n```", msg, remaining);
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
