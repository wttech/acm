package com.vml.es.aem.acm.core.code;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ExecutionId {

    private ExecutionId() {
        // intentionally empty
    }

    /**
     * Generate something looking Sling job compatible e.g '2024/12/17/22/3/7e553244-1dc4-4cb5-a9d6-b384c0d2d919_108'
     */
    public static String generate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm");
        String formattedDate = now.format(formatter);
        String uuid = UUID.randomUUID().toString();
        return String.format("%s-%s_%d", formattedDate, uuid, now.getNano());
    }
}
