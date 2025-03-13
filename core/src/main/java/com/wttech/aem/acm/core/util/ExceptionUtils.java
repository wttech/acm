package com.wttech.aem.acm.core.util;

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
}
