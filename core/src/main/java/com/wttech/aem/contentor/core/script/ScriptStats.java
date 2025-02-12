package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.code.Execution;
import com.wttech.aem.contentor.core.code.ExecutionStatus;
import org.apache.sling.api.resource.ResourceResolver;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class ScriptStats implements Serializable {

    private final String scriptId;

    private Map<ExecutionStatus, Integer> statusCount;

    private Execution lastExecution;

    public ScriptStats(String scriptId, Map<ExecutionStatus, Integer> statusCount, Execution lastExecution) {
        this.scriptId = scriptId;
        this.statusCount = statusCount;
        this.lastExecution = lastExecution;
    }

    public static ScriptStats computeById(ResourceResolver resourceResolver, String scriptId) {
        return new ScriptStats(scriptId, Collections.emptyMap(), null);
    }

    public String getScriptId() {
        return scriptId;
    }

    public Map<ExecutionStatus, Integer> getStatusCount() {
        return statusCount;
    }

    public Execution getLastExecution() {
        return lastExecution;
    }
}
