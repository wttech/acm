package dev.vml.es.acm.core.event;

import java.util.Arrays;
import java.util.Optional;

public enum EventType {
    EXECUTION_QUEUE_RESET,
    HISTORY_CLEAR;

    public static Optional<EventType> of(String name) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
