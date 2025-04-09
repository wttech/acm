package com.wttech.aem.acm.core.script;

import com.wttech.aem.acm.core.code.Execution;
import com.wttech.aem.acm.core.code.ExecutionHistory;
import com.wttech.aem.acm.core.code.ExecutionQuery;
import com.wttech.aem.acm.core.code.ExecutionStatus;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.sling.api.resource.ResourceResolver;

public class ScriptStats implements Serializable {

    private final String path;

    private Map<ExecutionStatus, Long> statusCount;

    private Execution lastExecution;

    public ScriptStats(String path, Map<ExecutionStatus, Long> statusCount, Execution lastExecution) {
        this.path = path;
        this.statusCount = statusCount;
        this.lastExecution = lastExecution;
    }

    public static ScriptStats forCompletedByPath(ResourceResolver resourceResolver, String path, int limit) {
        AtomicReference<Execution> lastExecution = new AtomicReference<>();
        Map<ExecutionStatus, Long> statusCount =
                ExecutionStatus.completed().stream().collect(HashMap::new, (m, s) -> m.put(s, 0L), HashMap::putAll);

        ExecutionHistory history = new ExecutionHistory(resourceResolver);
        ExecutionQuery query = new ExecutionQuery();
        query.setStatuses(ExecutionStatus.completed());

        ScriptType scriptType = ScriptType.byPath(path)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Script type for path '%s' cannot be determined!", path)));
        switch (scriptType) {
            case MANUAL:
            case ENABLED:
                query.setExecutableId(path);
                break;
            case DISABLED:
                query.setExecutableId(ScriptType.ENABLED.enforcePath(path));
                break;
            case EXTENSION:
                return new ScriptStats(path, Collections.emptyMap(), null);
            default:
                throw new IllegalStateException(String.format(
                        "Script type '%s' for path '%s' is not supported to calculate stats!", scriptType, path));
        }

        history.findAll(query).limit(limit).forEach(e -> {
            if (lastExecution.get() == null) {
                lastExecution.set(e);
            }
            statusCount.put(e.getStatus(), statusCount.get(e.getStatus()) + 1);
        });

        return new ScriptStats(path, statusCount, lastExecution.get());
    }

    public String getPath() {
        return path;
    }

    public Map<ExecutionStatus, Long> getStatusCount() {
        return statusCount;
    }

    public Execution getLastExecution() {
        return lastExecution;
    }
}
