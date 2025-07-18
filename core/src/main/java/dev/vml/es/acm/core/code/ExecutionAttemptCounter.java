package dev.vml.es.acm.core.code;

import org.osgi.service.component.annotations.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = ExecutionAttemptCounter.class, immediate = true)
public class ExecutionAttemptCounter {

    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    private AtomicLong getOrPut(String executableId) {
        return counters.computeIfAbsent(executableId, id -> new AtomicLong(0));
    }

    public long incrementAndGet(String executableId) {
        return getOrPut(executableId).incrementAndGet();
    }

    public long get(String executableId) {
        return getOrPut(executableId).get();
    }

    public void reset(String executableId) {
        counters.remove(executableId);
    }
}