package dev.vml.es.acm.core.code.log;

import java.util.function.Consumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * Log interceptor using OSGi Log Service 1.4 API.
 * Available on AEM 6.6+ and AEMaaCS where LogEntry.getLoggerName() exists.
 */
@Component(service = LogInterceptor.class, property = "type=" + LogInterceptor.TYPE_NATIVE)
public class NativeLogInterceptor implements LogInterceptor {

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
        if (!isAvailable()) {
            return () -> {};
        }

        LogListener logListener = entry -> {
            String loggerName = entry.getLoggerName();
            if (loggerName != null && matchesAny(loggerName, loggerNames)) {
                LogMessage message = new LogMessage(
                        loggerName, levelToString(entry.getLogLevel()), entry.getMessage(), entry.getTime());
                listener.accept(message);
            }
        };

        logReaderService.addLogListener(logListener);
        return () -> logReaderService.removeLogListener(logListener);
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
