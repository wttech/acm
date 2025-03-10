package com.wttech.aem.contentor.core.code;

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
        Optional<Execution> execution = queue.read(id);
        if (execution.isPresent()) {
            return execution;
        } else {
            return history.read(id);
        }
    }

    public Stream<Execution> readAll(Collection<String> ids) {
        return (ids != null ? ids.stream() : Stream.<String>empty())
                .map(this::read)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
}
