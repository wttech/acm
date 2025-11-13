package dev.vml.es.acm.core.code;

import java.util.Arrays;
import java.util.Optional;

public enum CodePrintLevel {
    INFO,
    ERROR,
    WARN,
    DEBUG,
    TRACE;

    public static Optional<CodePrintLevel> find(String level) {
        return Arrays.stream(values())
                .filter(l -> l.name().equalsIgnoreCase(level))
                .findFirst();
    }

    public static CodePrintLevel of(String level) {
        return find(level)
                .orElseThrow(() ->
                        new IllegalArgumentException(String.format("Code print level '%s' is not supported!", level)));
    }
}
