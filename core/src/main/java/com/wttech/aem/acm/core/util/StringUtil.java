package com.wttech.aem.acm.core.util;

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
}
