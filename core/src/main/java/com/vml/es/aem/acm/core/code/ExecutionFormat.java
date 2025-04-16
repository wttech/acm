package com.vml.es.aem.acm.core.code;

import java.util.Arrays;
import java.util.Optional;

public enum ExecutionFormat {
    SUMMARY,
    FULL;

    public static Optional<ExecutionFormat> of(String text) {
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(text))
                .findFirst();
    }
}
