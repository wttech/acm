package dev.vml.es.acm.core.code.log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects the best available {@link LogInterceptor} implementation.
 *
 * <p><strong>Why not OSGi Log Service 1.4?</strong> Its {@code LogReaderService} only captures
 * logs sent directly to OSGi Log Service. SLF4J/Logback logs are bridged one-way (SLF4J → OSGi),
 * so {@code LogReaderService} doesn't receive them. We use Sling AppenderTracker instead.</p>
 *
 * @see SlingLogInterceptor
 */
@Component(service = LogInterceptorManager.class)
public class LogInterceptorManager {

    private static final Logger LOG = LoggerFactory.getLogger(LogInterceptorManager.class);

    private final List<LogInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @Reference(
            service = LogInterceptor.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindInterceptor(LogInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    protected void unbindInterceptor(LogInterceptor interceptor) {
        interceptors.remove(interceptor);
    }

    public LogInterceptor.Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        return interceptors.stream()
                .filter(LogInterceptor::isAvailable)
                .findFirst()
                .map(i -> {
                    LOG.debug("Using log interceptor: {}", i.getClass().getSimpleName());
                    return i.attach(listener, loggerNames);
                })
                .orElseGet(() -> {
                    LOG.warn("No log interceptor available");
                    return () -> {};
                });
    }

    public boolean isAvailable() {
        return interceptors.stream().anyMatch(LogInterceptor::isAvailable);
    }
}
