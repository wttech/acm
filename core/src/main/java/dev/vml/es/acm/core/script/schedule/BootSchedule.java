package dev.vml.es.acm.core.script.schedule;

import dev.vml.es.acm.core.script.ScriptSchedule;

import java.util.concurrent.atomic.AtomicBoolean;

public class BootSchedule implements ScriptSchedule {

    private final AtomicBoolean done = new AtomicBoolean(false);

    @Override
    public String getId() {
        return "boot";
    }

    public AtomicBoolean getDone() {
        return done;
    }
}
