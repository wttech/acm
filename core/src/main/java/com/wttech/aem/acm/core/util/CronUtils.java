package com.wttech.aem.acm.core.util;

import com.wttech.aem.acm.core.util.quartz.CronExpression;
import java.text.ParseException;
import java.util.Date;

public final class CronUtils {
    private CronUtils() {
        // intentionally empty
    }

    /** This function returns time between next 2 runs of provided cron job. If cron expression is invalid returned value will be -1. */
    public static long getIntervalBetweenRuns(String cronExpression) {
        try {
            CronExpression expression = new CronExpression(cronExpression);
            Date now = new Date();
            Date nextRun = expression.getNextValidTimeAfter(now);
            if (nextRun == null) {
                return -1;
            }
            Date secondRun = expression.getNextValidTimeAfter(nextRun);
            if (secondRun == null) {
                return -1;
            }
            return secondRun.getTime() - nextRun.getTime();
        } catch (ParseException e) {
            System.err.println("Invalid cron expression: " + e.getMessage());
            return -1;
        }
    }
}
