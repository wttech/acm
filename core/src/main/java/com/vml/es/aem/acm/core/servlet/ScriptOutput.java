package com.vml.es.aem.acm.core.servlet;

import com.vml.es.aem.acm.core.script.Script;
import com.vml.es.aem.acm.core.script.ScriptStats;
import java.io.Serializable;
import java.util.List;

public class ScriptOutput implements Serializable {

    private final List<Script> list;

    private final List<ScriptStats> stats;

    public ScriptOutput(List<Script> scripts, List<ScriptStats> stats) {
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
