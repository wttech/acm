package dev.vml.es.acm.core.code;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.sling.api.resource.ResourceResolver;

public class ExecutionResolver {

    private final ExecutionQueue queue;

    private final ExecutionHistory history;

    public ExecutionResolver(ExecutionQueue queue, ExecutionHistory history) {
        this.queue = queue;
        this.history = history;
    }

    public ExecutionResolver(ExecutionQueue queue, ResourceResolver resourceResolver) {
        this(queue, new ExecutionHistory(resourceResolver));
    }

    public Optional<Execution> resolve(String id) {
        Optional<Execution> execution = history.read(id);
        if (execution.isPresent()) {
            return execution;
        } else {
            return queue.read(id);
        }
    }

    public Optional<ExecutionSummary> resolveSummary(String id) {
        Optional<ExecutionSummary> execution = history.readSummary(id);
        if (execution.isPresent()) {
            return execution;
        } else {
            return queue.readSummary(id);
        }
    }

    public Stream<Execution> resolveAll(Collection<String> ids) {
        return (ids != null ? ids.stream() : Stream.<String>empty())
                .map(this::resolve)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<ExecutionSummary> resolveAllSummaries(Collection<String> ids) {
        return (ids != null ? ids.stream() : Stream.<String>empty())
                .map(this::resolveSummary)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
}
