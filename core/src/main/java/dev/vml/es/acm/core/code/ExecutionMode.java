package dev.vml.es.acm.core.code;

import java.util.Arrays;
import java.util.Optional;

public enum ExecutionMode {
    PARSE,
    CHECK,
    RUN;

    public static Optional<ExecutionMode> of(String text) {
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(text))
                .findFirst();
    }
}
