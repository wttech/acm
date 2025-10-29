package dev.vml.es.acm.core.script;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;

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
