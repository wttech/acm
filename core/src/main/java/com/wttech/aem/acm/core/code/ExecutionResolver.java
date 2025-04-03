package com.wttech.aem.acm.core.code;

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

    public Optional<Execution> read(String id) {
        Optional<Execution> execution = history.read(id);
        if (execution.isPresent()) {
            return execution;
        } else {
            return queue.read(id);
        }
    }

    public Optional<ExecutionSummary> readSummary(String id) {
        Optional<ExecutionSummary> execution = history.readSummary(id);
        if (execution.isPresent()) {
            return execution;
        } else {
            return queue.readSummary(id);
        }
    }

    public Stream<Execution> readAll(Collection<String> ids) {
        return (ids != null ? ids.stream() : Stream.<String>empty())
                .map(this::read)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<ExecutionSummary> readAllSummaries(Collection<String> ids) {
        return (ids != null ? ids.stream() : Stream.<String>empty())
                .map(this::readSummary)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
}
