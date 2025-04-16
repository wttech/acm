package com.vml.es.aem.acm.core.util;

import java.util.Date;

public final class NumberUtils {

    private NumberUtils() {
        // intentionally empty
    }

    public static long durationBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0L;
        }
        return endDate.getTime() - startDate.getTime();
    }
}
