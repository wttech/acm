package dev.vml.es.acm.core.script.schedule;

import dev.vml.es.acm.core.script.ScriptSchedule;

public class CronSchedule implements ScriptSchedule {

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
