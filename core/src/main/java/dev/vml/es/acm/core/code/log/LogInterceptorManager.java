package dev.vml.es.acm.core.code.log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages log interceptors, selecting the best available implementation.
 * Designed for extensibility - allows adding alternative implementations in the future.
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
