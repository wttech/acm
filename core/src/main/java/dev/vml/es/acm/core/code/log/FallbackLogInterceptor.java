package dev.vml.es.acm.core.code.log;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log interceptor using Logback API via reflection.
 * Fallback for environments where Sling Commons Log AppenderTracker is not available.
 * Manually attaches appender to ROOT logger.
 */
@Component(service = LogInterceptor.class, property = "type=" + LogInterceptor.TYPE_FALLBACK)
public class FallbackLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(FallbackLogInterceptor.class);

    private static final String APPENDER_NAME = "ACM-FallbackLogInterceptor";

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
            cl.loadClass(LogbackAppenderFactory.LOGBACK_LOGGER_CONTEXT);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            LOG.debug("Unexpected error checking Logback availability", e);
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Fallback log interceptor cannot attach - invalid parameters: listener={}, loggerNames={}",
                    listener, loggerNames);
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.warn("Fallback log interceptor is not available - Logback classes not found");
            return () -> {};
        }

        try {
            return doAttach(listener, loggerNames);
        } catch (Exception e) {
            LOG.error("Failed to attach fallback log interceptor", e);
            return () -> {};
        }
    }

    private Handle doAttach(Consumer<LogMessage> listener, String[] loggerNames) throws Exception {
        ClassLoader cl = classLoaderManager.getDynamicClassLoader();
        LogbackAppenderFactory factory = new LogbackAppenderFactory(cl);

        List<String> loggerNameList = Arrays.asList(loggerNames);
        Object appender = factory.createAppender(APPENDER_NAME, listener, loggerNameList);

        Object loggerContext = factory.getLoggerContext();
        Object rootLogger = factory.getLogger(loggerContext, LogbackAppenderFactory.LOGBACK_ROOT_LOGGER);

        if (rootLogger == null) {
            throw new IllegalStateException("Could not obtain root logger from logging context");
        }

        factory.setContext(appender, loggerContext);
        factory.start(appender);
        factory.addAppender(rootLogger, appender);

        LOG.debug("Fallback log interceptor attached for loggers: {}", loggerNameList);

        return () -> {
            try {
                factory.stop(appender);
                factory.detachAppender(rootLogger, appender);
            } catch (Exception e) {
                LOG.warn("Failed to detach fallback log appender", e);
            }
        };
    }
}
