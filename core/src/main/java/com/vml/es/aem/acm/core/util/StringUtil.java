package com.vml.es.aem.acm.core.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;

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

    public static Map<String, String> toString(ValueMap vm) {
        return vm.entrySet().stream()
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), vm.get(e.getKey(), String.class)), HashMap::putAll);
    }
}
