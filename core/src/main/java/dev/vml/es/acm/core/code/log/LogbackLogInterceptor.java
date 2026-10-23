package dev.vml.es.acm.core.code.log;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = LogInterceptor.class)
public class LogbackLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackLogInterceptor.class);

    private static final String APPENDER_NAME = "ACM-LogbackLogInterceptor";

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
            LogbackAppenderFactory.Attachment attachment = factory.attach(APPENDER_NAME, listener, loggerList);
            LOG.debug("Attached appender to loggers: {}", attachment.getAttachedLoggers());
            return attachment::detach;
        } catch (Exception e) {
            LOG.error("Failed to attach log interceptor", e);
            return () -> {};
        }
    }

    private LogbackAppenderFactory getFactory() {
        try {
            Object loggerContext = LoggerFactory.getILoggerFactory();
            if (loggerContext == null) {
                LOG.debug("Cannot obtain SLF4J ILoggerFactory");
                return null;
            }
            ClassLoader cl = loggerContext.getClass().getClassLoader();
            if (cl == null) {
                LOG.debug(
                        "SLF4J ILoggerFactory '{}' has no class loader",
                        loggerContext.getClass().getName());
                return null;
            }
            return new LogbackAppenderFactory(cl, loggerContext);
        } catch (Exception e) {
            LOG.debug("Logback log interceptor is not available", e);
            return null;
        }
    }
}
