package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;

public class Schedules {

    public BootSchedule boot() {
        return new BootSchedule();
    }

    public CronSchedule cron(String cronExpression) {
        return new CronSchedule(cronExpression);
    }
}
