package com.wttech.aem.contentor.core.assist;

import java.util.Arrays;

public enum SuggestionType {
    RESOURCE, CLASS, VARIABLE, ALL;

    public static SuggestionType of(String type) {
           return Arrays.stream(values())
                   .filter(v -> v.name().equalsIgnoreCase(type))
                   .findFirst()
                   .orElse(ALL);
    }
}
