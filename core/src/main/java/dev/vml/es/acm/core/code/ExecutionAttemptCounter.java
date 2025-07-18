package dev.vml.es.acm.core.code;

import org.osgi.service.component.annotations.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = ExecutionAttemptCounter.class, immediate = true)
public class ExecutionAttemptCounter {

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    private AtomicLong getOrPut(Executable executable) {
        return counters.computeIfAbsent(computeKey(executable), id -> new AtomicLong(0));
    }

    private String computeKey(Executable executable) {
        return String.format("%s_%s", executable.getId(), executable.getContent().hashCode());
    }

    public long incrementAndGet(Executable executable) {
        return getOrPut(executable).incrementAndGet();
    }

    public long get(Executable executable) {
        return getOrPut(executable).get();
    }

    public void reset(String executableId) {
        counters.entrySet().removeIf(entry -> entry.getKey().startsWith(executableId + "_"));
    }

    public void reset(Executable executable) {
        counters.remove(computeKey(executable));
    }
}