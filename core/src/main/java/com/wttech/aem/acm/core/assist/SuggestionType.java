package com.wttech.aem.acm.core.assist;

import java.util.Arrays;

public enum SuggestionType {
    RESOURCE,
    CLASS,
    VARIABLE,
    SNIPPET,
    ALL;

    public static SuggestionType of(String type) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(type))
                .findFirst()
                .orElse(ALL);
    }
}
