package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.code.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;

public class ScriptStats implements Serializable {

    private final String path;

    private final Map<ExecutionStatus, Long> statusCount;

    private final ExecutionSummary lastExecution;

    private final Long averageDuration;

    public ScriptStats(
            String path, Map<ExecutionStatus, Long> statusCount, ExecutionSummary lastExecution, Long averageDuration) {
        this.path = path;
        this.statusCount = statusCount;
        this.lastExecution = lastExecution;
        this.averageDuration = averageDuration;
    }

    public static ScriptStats forCompletedByPath(ResourceResolver resourceResolver, String path, long limit) {
        ScriptType scriptType = ScriptType.byPath(path)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Script type for path '%s' cannot be determined!", path)));
        if (!scriptType.statsSupported()) {
            return new ScriptStats(path, Collections.emptyMap(), null, null);
        }

        AtomicReference<ExecutionSummary> lastExecution = new AtomicReference<>();
        Map<ExecutionStatus, Long> statusCount =
                ExecutionStatus.completed().stream().collect(HashMap::new, (m, s) -> m.put(s, 0L), HashMap::putAll);

        ExecutionHistory history = new ExecutionHistory(resourceResolver);
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(path);
        query.setStatuses(ExecutionStatus.completed());

        List<ExecutionSummary> executions =
                history.findAllSummaries(query).limit(limit).collect(Collectors.toList());
        Integer length = executions.size();
        AtomicReference<Long> averageDuration = new AtomicReference<>(0L);

        executions.forEach(e -> {
            if (lastExecution.get() == null) {
                lastExecution.set(e);
            }
            statusCount.put(e.getStatus(), statusCount.get(e.getStatus()) + 1);
            averageDuration.updateAndGet(v -> v + e.getDuration());
        });

        return new ScriptStats(
                path, statusCount, lastExecution.get(), length == 0 ? 0 : averageDuration.get() / length);
    }

    public String getPath() {
        return path;
    }

    public Long getAverageDuration() {
        return averageDuration;
    }

    public Map<ExecutionStatus, Long> getStatusCount() {
        return statusCount;
    }

    public ExecutionSummary getLastExecution() {
        return lastExecution;
    }
}
