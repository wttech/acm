package com.wttech.aem.migrator.core.util;

public final class ExceptionUtils {

    private ExceptionUtils() {
        // intentionally empty
    }

    public static String toString(Throwable cause) {
        StringBuilder builder = new StringBuilder();
        if (cause != null) {
            Throwable rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(cause);
            if (rootCause != null && rootCause != cause) {
                builder.append(rootCause.getMessage()).append("\n");
                builder.append(org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(rootCause));
                builder.append("\n\n");
            }

            builder.append(cause.getMessage()).append("\n");
            builder.append(org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(cause));
        }
        return builder.toString();
    }
}
