package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.code.schedule.NoneSchedule;

public class Schedules {

    public NoneSchedule none() {
        return new NoneSchedule();
    }

    public BootSchedule boot() {
        return new BootSchedule();
    }

    public CronSchedule cron(String cronExpression) {
        return new CronSchedule(cronExpression);
    }
}
