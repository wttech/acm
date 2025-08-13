package dev.vml.es.acm.core.code.schedule;

import dev.vml.es.acm.core.code.Schedule;

public class NoneSchedule implements Schedule {

    @Override
    public String getId() {
        return "none";
    }
}
