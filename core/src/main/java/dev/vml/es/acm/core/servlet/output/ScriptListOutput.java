package dev.vml.es.acm.core.servlet.output;

import dev.vml.es.acm.core.script.Script;
import dev.vml.es.acm.core.script.ScriptSchedule;
import dev.vml.es.acm.core.script.ScriptStats;
import java.io.Serializable;
import java.util.List;

public class ScriptListOutput implements Serializable {

    private final List<Script> list;

    private final List<ScriptStats> stats;

    private final List<ScriptSchedule> schedules;

    public ScriptListOutput(List<Script> scripts, List<ScriptStats> stats, List<ScriptSchedule> schedules) {
        this.list = scripts;
        this.stats = stats;
        this.schedules = schedules;
    }

    public List<Script> getList() {
        return list;
    }

    public List<ScriptStats> getStats() {
        return stats;
    }

    public List<ScriptSchedule> getSchedules() {
        return schedules;
    }
}
