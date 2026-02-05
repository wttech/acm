package dev.vml.es.acm.core.code.log;

import java.util.Arrays;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log interceptor using OSGi Log Service 1.4 API.
 * Available on AEM 6.6+ and AEMaaCS where LogEntry.getLoggerName() exists.
 */
@Component(service = LogInterceptor.class, property = "type=" + LogInterceptor.TYPE_NATIVE)
public class NativeLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(NativeLogInterceptor.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile LogReaderService logReaderService;

    @Override
    public boolean isAvailable() {
        if (logReaderService == null) {
            return false;
        }
        try {
            LogEntry.class.getMethod("getLoggerName");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Native log interceptor cannot attach - invalid parameters: listener={}, loggerNames={}", listener, loggerNames);
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.warn("Native log interceptor is not available - OSGi Log Service (>= 1.4) not found");
            return () -> {};
        }

        LogListener logListener = entry -> {
            try {
                String loggerName = entry.getLoggerName();
                if (loggerName != null && matchesAny(loggerName, loggerNames)) {
                    LogMessage message = new LogMessage(
                            loggerName, levelToString(entry.getLogLevel()), entry.getMessage(), entry.getTime());
                    listener.accept(message);
                }
            } catch (Exception e) {
                // Silently ignore - we don't want to disrupt logging
            }
        };

        try {
            logReaderService.addLogListener(logListener);
            LOG.debug("Native log interceptor attached for loggers: {}", Arrays.asList(loggerNames));
        } catch (Exception e) {
            LOG.error("Failed to attach native log interceptor", e);
            return () -> {};
        }

        return () -> {
            try {
                logReaderService.removeLogListener(logListener);
            } catch (Exception e) {
                LOG.warn("Failed to detach native log listener", e);
            }
        };
    }

    private boolean matchesAny(String loggerName, String[] prefixes) {
        for (String prefix : prefixes) {
            if (loggerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String levelToString(org.osgi.service.log.LogLevel level) {
        return level != null ? level.name() : "INFO";
    }
}
