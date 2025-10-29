package dev.vml.es.acm.core.script;

import java.io.Serializable;
import java.util.Date;

public class ScriptSchedule implements Serializable {

    private final String path;

    private final Date nextExecution;

    public ScriptSchedule(String path, Date nextExecution) {
        this.path = path;
        this.nextExecution = nextExecution;
    }

    public String getPath() {
        return path;
    }

    public Date getNextExecution() {
        return nextExecution;
    }
}
