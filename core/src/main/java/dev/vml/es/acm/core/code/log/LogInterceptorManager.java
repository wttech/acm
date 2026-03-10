package dev.vml.es.acm.core.code.log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                    LOG.debug("No log interceptor available");
                    return () -> {};
                });
    }

    public boolean isAvailable() {
        return interceptors.stream().anyMatch(LogInterceptor::isAvailable);
    }
}
