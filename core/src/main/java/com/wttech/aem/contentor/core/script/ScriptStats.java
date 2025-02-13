package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.code.Execution;
import com.wttech.aem.contentor.core.code.ExecutionHistory;
import com.wttech.aem.contentor.core.code.ExecutionQuery;
import com.wttech.aem.contentor.core.code.ExecutionStatus;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;

public class ScriptStats implements Serializable {

    private final String scriptId;

    private Map<ExecutionStatus, Long> statusCount;

    private Execution lastExecution;

    public ScriptStats(String scriptId, Map<ExecutionStatus, Long> statusCount, Execution lastExecution) {
        this.scriptId = scriptId;
        this.statusCount = statusCount;
        this.lastExecution = lastExecution;
    }

    public static ScriptStats computeById(ResourceResolver resourceResolver, String scriptId, int limit) {
        AtomicReference<Execution> lastExecution = new AtomicReference<>(); // TODO calculate it separately (does not rely on limit)
        Map<ExecutionStatus, Long> statusCount =
                Arrays.stream(ExecutionStatus.values()).collect(HashMap::new, (m, s) -> m.put(s, 0L), HashMap::putAll);

        ExecutionHistory history = new ExecutionHistory(resourceResolver);
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(
                StringUtils.replace(scriptId, "/disabled/", "/enabled/")); // TODO change path more smartly
        history.findAll(query).limit(limit).forEach(e -> {
            if (lastExecution.get() == null) {
                lastExecution.set(e);
            }
            statusCount.put(e.getStatus(), statusCount.get(e.getStatus()) + 1);
        });

        return new ScriptStats(scriptId, statusCount, lastExecution.get());
    }

    public String getScriptId() {
        return scriptId;
    }

    public Map<ExecutionStatus, Long> getStatusCount() {
        return statusCount;
    }

    public Execution getLastExecution() {
        return lastExecution;
    }
}
