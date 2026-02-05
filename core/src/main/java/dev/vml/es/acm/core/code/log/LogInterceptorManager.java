package dev.vml.es.acm.core.code.log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages log interceptors, selecting the best available implementation.
 * Priority: OSGi Log Service 1.4 (clean API) > Logback reflection (fallback).
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
        LogInterceptor nativeInterceptor = findInterceptor(LogInterceptor.TYPE_NATIVE);
        if (nativeInterceptor != null && nativeInterceptor.isAvailable()) {
            LOG.debug("Using native log interception (OSGi Log Service)");
            return nativeInterceptor.attach(listener, loggerNames);
        }

        LogInterceptor fallbackInterceptor = findInterceptor(LogInterceptor.TYPE_FALLBACK);
        if (fallbackInterceptor != null && fallbackInterceptor.isAvailable()) {
            LOG.debug("Using fallback log interception (Logback reflection)");
            return fallbackInterceptor.attach(listener, loggerNames);
        }

        LOG.warn("No log interceptor available on this AEM instance");
        return () -> {};
    }

    private LogInterceptor findInterceptor(String type) {
        return interceptors.stream()
                .filter(i -> i.getClass().getSimpleName().toLowerCase().contains(type))
                .findFirst()
                .orElse(null);
    }

    public boolean isAvailable() {
        return interceptors.stream().anyMatch(LogInterceptor::isAvailable);
    }
}
