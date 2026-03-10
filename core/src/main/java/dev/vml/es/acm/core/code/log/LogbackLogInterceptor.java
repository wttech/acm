package dev.vml.es.acm.core.code.log;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = LogInterceptor.class)
public class LogbackLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackLogInterceptor.class);

    private static final String APPENDER_NAME = "ACM-LogbackLogInterceptor";

    @Reference
    private DynamicClassLoaderManager classLoaderManager;

    @Override
    public boolean isAvailable() {
        if (classLoaderManager == null) {
            return false;
        }
        try {
            ClassLoader cl = classLoaderManager.getDynamicClassLoader();
            if (cl == null) {
                return false;
            }
            return new LogbackAppenderFactory(cl).isAvailable();
        } catch (Exception e) {
            LOG.debug("Cannot check log interceptor availability", e);
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Cannot attach - invalid parameters");
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.warn("Log interceptor not available");
            return () -> {};
        }
        try {
            LogbackAppenderFactory factory = new LogbackAppenderFactory(classLoaderManager.getDynamicClassLoader());
            List<String> loggerList = Arrays.asList(loggerNames);
            Object appender = factory.attach(APPENDER_NAME, listener, loggerList);
            LOG.debug("Attached appender to loggers: {}", loggerList);

            return () -> {
                try {
                    factory.detach(appender, loggerList);
                } catch (Exception e) {
                    LOG.debug("Failed to detach appender", e);
                }
            };
        } catch (Exception e) {
            LOG.error("Failed to attach log interceptor", e);
            return () -> {};
        }
    }
}
