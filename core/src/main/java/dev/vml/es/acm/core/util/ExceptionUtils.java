package dev.vml.es.acm.core.util;

import java.util.Optional;

public final class ExceptionUtils {

    private ExceptionUtils() {
        // intentionally empty
    }

    public static String toString(Throwable cause) {
        return Optional.ofNullable(cause)
                .map(org.apache.commons.lang3.exception.ExceptionUtils::getStackTrace)
                .orElse(null);
    }

    public static Throwable getRootCause(Throwable throwable) {
        return org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(throwable);
    }
}
