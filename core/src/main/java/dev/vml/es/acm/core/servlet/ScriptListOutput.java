package dev.vml.es.acm.core.servlet;

import dev.vml.es.acm.core.script.Script;
import dev.vml.es.acm.core.script.ScriptStats;
import java.io.Serializable;
import java.util.List;

public class ScriptListOutput implements Serializable {

    private final List<Script> list;

    private final List<ScriptStats> stats;

    public ScriptListOutput(List<Script> scripts, List<ScriptStats> stats) {
        this.list = scripts;
        this.stats = stats;
    }

    public List<Script> getList() {
        return list;
    }

    public List<ScriptStats> getStats() {
        return stats;
    }
}
