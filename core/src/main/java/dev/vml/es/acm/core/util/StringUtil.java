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
}
