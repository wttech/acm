package dev.vml.es.acm.core.code;

public class ScheduleResult {

    private final Execution execution;

    private final Schedule schedule;

    public ScheduleResult(Execution execution, Schedule schedule) {
        this.execution = execution;
        this.schedule = schedule;
    }

    public Execution getExecution() {
        return execution;
    }

    public Schedule getSchedule() {
        return schedule;
    }
}
