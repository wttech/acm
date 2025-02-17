package com.wttech.aem.contentor.core.servlet;

import com.wttech.aem.contentor.core.script.Script;
import com.wttech.aem.contentor.core.script.ScriptStats;
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
