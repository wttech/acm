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
        return getFactory() != null;
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Cannot attach - invalid parameters");
            return () -> {};
        }
        LogbackAppenderFactory factory = getFactory();
        if (factory == null) {
            return () -> {};
        }
        try {
            List<String> loggerList = Arrays.asList(loggerNames);
            Object appender = factory.attach(APPENDER_NAME, listener, loggerList);
            LOG.debug("Attached appender to loggers: {}", loggerList);
            return () -> factory.detach(appender, loggerList);
        } catch (Exception e) {
            LOG.error("Failed to attach log interceptor", e);
            return () -> {};
        }
    }

    private LogbackAppenderFactory getFactory() {
        if (classLoaderManager == null) {
            return null;
        }
        try {
            ClassLoader cl = classLoaderManager.getDynamicClassLoader();
            if (cl == null) {
                return null;
            }
            return new LogbackAppenderFactory(cl);
        } catch (Exception e) {
            return null;
        }
    }
}
