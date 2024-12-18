package com.wttech.aem.contentor.core.util;

public final class ExceptionUtils {

    private ExceptionUtils() {
        // intentionally empty
    }

    public static String toString(Throwable cause) {
        return org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(cause);
    }
}
