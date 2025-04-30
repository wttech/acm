package com.vml.es.aem.acm.core.script;

import com.vml.es.aem.acm.core.code.Execution;
import com.vml.es.aem.acm.core.code.ExecutionHistory;
import com.vml.es.aem.acm.core.code.ExecutionQuery;
import com.vml.es.aem.acm.core.code.ExecutionStatus;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.resource.ResourceResolver;

public class ScriptStats implements Serializable {

    private final String path;

    private final Map<ExecutionStatus, Long> statusCount;

    private final Execution lastExecution;

    private final Long averageExecutionTime;

    public ScriptStats(String path, Map<ExecutionStatus, Long> statusCount, Execution lastExecution, Long averageExecutionTime) {
        this.path = path;
        this.statusCount = statusCount;
        this.lastExecution = lastExecution;
        this.averageExecutionTime = averageExecutionTime;
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
                return new ScriptStats(path, Collections.emptyMap(), null, null);
            default:
                throw new IllegalStateException(String.format(
                        "Script type '%s' for path '%s' is not supported to calculate stats!", scriptType, path));
        }

        List<Execution> executions = history.findAll(query).limit(limit).collect(Collectors.toList());
        Integer length = executions.size();
        AtomicReference<Long> sumExecTime = new AtomicReference<>(0L);

        executions.forEach(e -> {
            if (lastExecution.get() == null) {
                lastExecution.set(e);
            }
            statusCount.put(e.getStatus(), statusCount.get(e.getStatus()) + 1);
            sumExecTime.updateAndGet(v -> v + e.getDuration());
        });

        return new ScriptStats(path, statusCount, lastExecution.get(), length == 0 ? 0 : sumExecTime.get() / length);
    }

    public String getPath() {
        return path;
    }

    public Long getAverageExecutionTime() {
        return averageExecutionTime;
    }

    public Map<ExecutionStatus, Long> getStatusCount() {
        return statusCount;
    }

    public Execution getLastExecution() {
        return lastExecution;
    }
}
