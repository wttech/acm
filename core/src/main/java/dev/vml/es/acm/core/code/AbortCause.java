package dev.vml.es.acm.core.code;

import java.util.Arrays;
import java.util.Optional;

public enum AbortCause {
    INSTANCE_SHUTDOWN("Execution aborted, execution queue is unavailable (instance or ACM is stopping)"),
    EXECUTION_LOCK_LOST("Execution aborted, its execution lock is held by another run or could not be confirmed");

    private final String message;

    AbortCause(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static Optional<AbortCause> of(String name) {
        return Arrays.stream(values()).filter(c -> c.name().equals(name)).findFirst();
    }
}
