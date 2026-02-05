package dev.vml.es.acm.core.code.log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages log interceptors, selecting the best available implementation.
 *
 * <p>This manager is designed for extensibility, allowing alternative log interception
 * implementations to be registered as OSGi services. Currently, the primary implementation
 * is {@link SlingLogInterceptor} which uses the Sling Commons Log AppenderTracker mechanism.</p>
 *
 * <p><strong>Why not OSGi Log Service 1.4?</strong></p>
 * <p>While OSGi Log Service 1.4 provides a {@code LogReaderService} for tracking log entries,
 * it only captures logs sent directly to the OSGi Log Service. SLF4J/Logback logs are bridged
 * <em>to</em> the OSGi Log Service (SLF4J → OSGi), but not the other way around.
 * The {@code LogReaderService} does not receive logs from SLF4J/Logback loggers, which is
 * what AEM applications typically use. Therefore, we use the Sling AppenderTracker mechanism
 * instead, which directly hooks into Logback.</p>
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
        LogInterceptor interceptor = findAvailableInterceptor();
        if (interceptor != null) {
            LOG.debug("Using log interceptor: {}", interceptor.getClass().getSimpleName());
            return interceptor.attach(listener, loggerNames);
        }

        LOG.warn("No log interceptor available on this AEM instance");
        return () -> {};
    }

    private LogInterceptor findAvailableInterceptor() {
        return interceptors.stream()
                .filter(LogInterceptor::isAvailable)
                .findFirst()
                .orElse(null);
    }

    public boolean isAvailable() {
        return interceptors.stream().anyMatch(LogInterceptor::isAvailable);
    }
}
