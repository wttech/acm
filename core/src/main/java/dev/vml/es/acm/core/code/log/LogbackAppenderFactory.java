package dev.vml.es.acm.core.code.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogbackAppenderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackAppenderFactory.class);

    private static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";
    private static final String LOGBACK_APPENDER = "ch.qos.logback.core.Appender";
    private static final String LOGBACK_CONTEXT = "ch.qos.logback.core.Context";
    private static final String LOGBACK_LOGGING_EVENT = "ch.qos.logback.classic.spi.ILoggingEvent";

    private final ClassLoader classLoader;
    private final Object loggerContext;
    private final Class<?> loggerContextClass;
    private final Class<?> appenderClass;
    private final Class<?> contextClass;
    private final Class<?> eventClass;

    LogbackAppenderFactory(ClassLoader classLoader, Object loggerContext) throws ClassNotFoundException {
        this.classLoader = classLoader;
        this.loggerContextClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        if (loggerContext == null || !loggerContextClass.isInstance(loggerContext)) {
            throw new IllegalStateException("SLF4J is not using Logback");
        }
        this.loggerContext = loggerContext;
        this.appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        this.contextClass = classLoader.loadClass(LOGBACK_CONTEXT);
        this.eventClass = classLoader.loadClass(LOGBACK_LOGGING_EVENT);
    }

    Attachment attach(String name, Consumer<LogMessage> listener, List<String> loggerNames)
            throws ReflectiveOperationException {
        Object appender = createAppender(name, listener, loggerNames);
        List<String> targetLoggers = resolveTargetLoggers(loggerNames);
        List<String> attached = new ArrayList<>();
        try {
            initAppender(appender, loggerContext);
            for (String loggerName : targetLoggers) {
                addAppenderToLogger(loggerContext, appender, loggerName);
                attached.add(loggerName);
            }
            return new Attachment(appender, attached);
        } catch (ReflectiveOperationException e) {
            detach(appender, attached);
            throw e;
        }
    }

    private List<String> resolveTargetLoggers(List<String> loggerNames) {
        Set<String> targets = new LinkedHashSet<>(loggerNames);
        try {
            List<?> existingLoggers =
                    (List<?>) loggerContextClass.getMethod("getLoggerList").invoke(loggerContext);
            if (existingLoggers != null) {
                for (Object logger : existingLoggers) {
                    String existingName =
                            (String) logger.getClass().getMethod("getName").invoke(logger);
                    Boolean additive =
                            (Boolean) logger.getClass().getMethod("isAdditive").invoke(logger);
                    if (Boolean.FALSE.equals(additive) && matchesPrefix(existingName, loggerNames)) {
                        targets.add(existingName);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Cannot inspect existing loggers for non-additive children", e);
        }
        return new ArrayList<>(targets);
    }

    void detach(Object appender, List<String> loggerNames) {
        if (appender == null) {
            return;
        }
        try {
            if (loggerNames != null) {
                for (String loggerName : loggerNames) {
                    try {
                        detachAppenderFromLogger(loggerContext, appender, loggerName);
                    } catch (Exception e) {
                        LOG.warn("Failed to detach appender from logger '{}'", loggerName, e);
                    }
                }
            }
        } finally {
            try {
                stopAppender(appender);
            } catch (Exception e) {
                LOG.warn("Failed to stop appender", e);
            }
        }
    }

    private Object createAppender(String name, Consumer<LogMessage> listener, List<String> loggerNames) {
        return Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] {appenderClass},
                new AppenderHandler(name, eventClass, listener, loggerNames));
    }

    private void initAppender(Object appender, Object loggerContext) throws ReflectiveOperationException {
        appenderClass.getMethod("setContext", contextClass).invoke(appender, loggerContext);
        appenderClass.getMethod("start").invoke(appender);
    }

    private void stopAppender(Object appender) throws ReflectiveOperationException {
        appenderClass.getMethod("stop").invoke(appender);
    }

    private void addAppenderToLogger(Object loggerContext, Object appender, String loggerName)
            throws ReflectiveOperationException {
        Object logger = loggerContextClass.getMethod("getLogger", String.class).invoke(loggerContext, loggerName);
        logger.getClass().getMethod("addAppender", appenderClass).invoke(logger, appender);
    }

    private void detachAppenderFromLogger(Object loggerContext, Object appender, String loggerName)
            throws ReflectiveOperationException {
        Object logger = loggerContextClass.getMethod("getLogger", String.class).invoke(loggerContext, loggerName);
        logger.getClass().getMethod("detachAppender", appenderClass).invoke(logger, appender);
    }

    private static boolean matchesPrefix(String loggerName, List<String> loggerPrefixes) {
        if (loggerName == null || loggerPrefixes == null) {
            return false;
        }
        for (String prefix : loggerPrefixes) {
            if (prefix != null && loggerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    class Attachment {
        private final Object appender;
        private final List<String> attachedLoggers;

        Attachment(Object appender, List<String> attachedLoggers) {
            this.appender = appender;
            this.attachedLoggers = attachedLoggers;
        }

        List<String> getAttachedLoggers() {
            return attachedLoggers;
        }

        void detach() {
            LogbackAppenderFactory.this.detach(appender, attachedLoggers);
        }
    }

    private static class AppenderHandler implements InvocationHandler {

        private final String appenderName;
        private final Class<?> eventClass;
        private final Consumer<LogMessage> listener;
        private final List<String> loggerPrefixes;
        private String name;
        private boolean started = true;
        private volatile Object lastEvent;

        AppenderHandler(String name, Class<?> eventClass, Consumer<LogMessage> listener, List<String> loggerPrefixes) {
            this.appenderName = name;
            this.name = name;
            this.eventClass = eventClass;
            this.listener = listener;
            this.loggerPrefixes = loggerPrefixes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String m = method.getName();
            switch (m) {
                case "doAppend":
                    if (args != null && args.length > 0 && args[0] != null) {
                        processEvent(args[0]);
                    }
                    return null;
                case "getName":
                    return name;
                case "setName":
                    if (args != null && args.length > 0 && args[0] instanceof String) {
                        name = (String) args[0];
                    }
                    return null;
                case "start":
                    started = true;
                    return null;
                case "stop":
                    started = false;
                    return null;
                case "isStarted":
                    return started;
                case "equals":
                    return proxy == (args != null && args.length > 0 ? args[0] : null);
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return appenderName + "@" + Integer.toHexString(System.identityHashCode(proxy));
                default:
                    return defaultValue(method.getReturnType());
            }
        }

        private void processEvent(Object event) {
            try {
                if (event == lastEvent) {
                    return;
                }
                lastEvent = event;
                String loggerName =
                        (String) eventClass.getMethod("getLoggerName").invoke(event);
                if (!matchesPrefix(loggerName, loggerPrefixes)) {
                    return;
                }
                String message =
                        (String) eventClass.getMethod("getFormattedMessage").invoke(event);
                String level = String.valueOf(eventClass.getMethod("getLevel").invoke(event));
                Long ts = (Long) eventClass.getMethod("getTimeStamp").invoke(event);
                listener.accept(
                        new LogMessage(loggerName, level, message, ts != null ? ts : System.currentTimeMillis()));
            } catch (Exception e) {
                LOG.trace("Failed to process log event", e);
            }
        }

        private Object defaultValue(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
