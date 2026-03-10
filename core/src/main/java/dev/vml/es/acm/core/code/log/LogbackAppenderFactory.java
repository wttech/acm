package dev.vml.es.acm.core.code.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
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

    LogbackAppenderFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    boolean isAvailable() {
        try {
            classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
            classLoader.loadClass(LOGBACK_APPENDER);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    Object attach(String name, Consumer<LogMessage> listener, List<String> loggerNames)
            throws ReflectiveOperationException {
        Object appender = createAppender(name, listener, loggerNames);
        List<String> attached = new ArrayList<>();
        try {
            initAppender(appender);
            for (String loggerName : loggerNames) {
                addAppenderToLogger(appender, loggerName);
                attached.add(loggerName);
            }
            return appender;
        } catch (ReflectiveOperationException e) {
            detach(appender, attached);
            throw e;
        }
    }

    void detach(Object appender, List<String> loggerNames) {
        if (appender == null) {
            return;
        }
        try {
            if (loggerNames != null) {
                for (String loggerName : loggerNames) {
                    try {
                        detachAppenderFromLogger(appender, loggerName);
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

    private Object createAppender(String name, Consumer<LogMessage> listener, List<String> loggerNames)
            throws ClassNotFoundException {
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        Class<?> eventClass = classLoader.loadClass(LOGBACK_LOGGING_EVENT);
        return Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] {appenderClass},
                new AppenderHandler(name, eventClass, listener, loggerNames));
    }

    private void initAppender(Object appender) throws ReflectiveOperationException {
        Object loggerContext = getLoggerContext();
        Class<?> ctxClass = classLoader.loadClass(LOGBACK_CONTEXT);
        appender.getClass().getMethod("setContext", ctxClass).invoke(appender, loggerContext);
        appender.getClass().getMethod("start").invoke(appender);
    }

    private void stopAppender(Object appender) throws ReflectiveOperationException {
        appender.getClass().getMethod("stop").invoke(appender);
    }

    private Object getLoggerContext() throws ClassNotFoundException {
        Class<?> ctxClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        Object ctx = org.slf4j.LoggerFactory.getILoggerFactory();
        if (ctx == null || !ctxClass.isInstance(ctx)) {
            throw new IllegalStateException("SLF4J is not using Logback");
        }
        return ctx;
    }

    private void addAppenderToLogger(Object appender, String loggerName) throws ReflectiveOperationException {
        Object loggerContext = getLoggerContext();
        Class<?> loggerContextClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        Object logger = loggerContextClass.getMethod("getLogger", String.class).invoke(loggerContext, loggerName);
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        logger.getClass().getMethod("addAppender", appenderClass).invoke(logger, appender);
    }

    private void detachAppenderFromLogger(Object appender, String loggerName) throws ReflectiveOperationException {
        Object loggerContext = getLoggerContext();
        Class<?> loggerContextClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        Object logger = loggerContextClass.getMethod("getLogger", String.class).invoke(loggerContext, loggerName);
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        logger.getClass().getMethod("detachAppender", appenderClass).invoke(logger, appender);
    }

    private static class AppenderHandler implements InvocationHandler {

        private final String appenderName;
        private final Class<?> eventClass;
        private final Consumer<LogMessage> listener;
        private final List<String> loggerPrefixes;
        private String name;
        private boolean started;

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
                String loggerName =
                        (String) eventClass.getMethod("getLoggerName").invoke(event);
                if (!matchesPrefix(loggerName)) {
                    return;
                }
                String message =
                        (String) eventClass.getMethod("getFormattedMessage").invoke(event);
                String level = String.valueOf(eventClass.getMethod("getLevel").invoke(event));
                Long ts = (Long) eventClass.getMethod("getTimeStamp").invoke(event);
                listener.accept(
                        new LogMessage(loggerName, level, message, ts != null ? ts : System.currentTimeMillis()));
            } catch (Exception ignored) {
                // Never disrupt logging
            }
        }

        private boolean matchesPrefix(String loggerName) {
            if (loggerName == null) {
                return false;
            }
            for (String prefix : loggerPrefixes) {
                if (prefix != null && loggerName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        private Object defaultValue(Class<?> type) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
