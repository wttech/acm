package dev.vml.es.acm.core.code.schedule;

import dev.vml.es.acm.core.code.Schedule;

public class CronSchedule implements Schedule {

    private final String expression;

    public CronSchedule(String expression) {
       this.expression = expression;
    }

    @Override
    public String getId() {
        return "cron";
    }

    public String getExpression() {
        return expression;
    }
}
