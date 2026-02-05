package dev.vml.es.acm.core.code.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;

/**
 * Creates Logback Appender proxies via reflection, avoiding compile-time dependency on ch.qos.logback.*.
 */
class LogbackAppenderFactory {

    static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";
    static final String LOGBACK_APPENDER = "ch.qos.logback.core.Appender";
    private static final String LOGBACK_CONTEXT = "ch.qos.logback.core.Context";
    private static final String LOGBACK_LOGGING_EVENT = "ch.qos.logback.classic.spi.ILoggingEvent";

    private final ClassLoader classLoader;

    LogbackAppenderFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    Object createAppender(String name, Consumer<LogMessage> listener, List<String> loggerNames)
            throws ClassNotFoundException {
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        Class<?> eventClass = classLoader.loadClass(LOGBACK_LOGGING_EVENT);
        return Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] {appenderClass},
                new AppenderHandler(name, eventClass, listener, loggerNames));
    }

    Object getLoggerContext() throws ClassNotFoundException {
        Class<?> ctxClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        Object ctx = org.slf4j.LoggerFactory.getILoggerFactory();
        if (ctx == null || !ctxClass.isInstance(ctx)) {
            throw new IllegalStateException("SLF4J is not using Logback");
        }
        return ctx;
    }

    void setContext(Object appender, Object context) throws ReflectiveOperationException {
        Class<?> ctxClass = classLoader.loadClass(LOGBACK_CONTEXT);
        appender.getClass().getMethod("setContext", ctxClass).invoke(appender, context);
    }

    void start(Object appender) throws ReflectiveOperationException {
        appender.getClass().getMethod("start").invoke(appender);
    }

    void stop(Object appender) throws ReflectiveOperationException {
        appender.getClass().getMethod("stop").invoke(appender);
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
