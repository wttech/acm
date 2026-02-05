package dev.vml.es.acm.core.code.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factory for creating Logback Appender proxies via reflection.
 * Avoids compile-time dependency on ch.qos.logback.* classes.
 */
class LogbackAppenderFactory {

    static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";
    static final String LOGBACK_LOGGER = "ch.qos.logback.classic.Logger";
    static final String LOGBACK_APPENDER = "ch.qos.logback.core.Appender";
    static final String LOGBACK_CONTEXT = "ch.qos.logback.core.Context";
    static final String LOGBACK_LOGGING_EVENT = "ch.qos.logback.classic.spi.ILoggingEvent";
    static final String LOGBACK_ROOT_LOGGER = "ROOT";

    private final ClassLoader classLoader;

    LogbackAppenderFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Creates a proxy Appender that forwards log events to the provided listener.
     *
     * @param appenderName unique name for the appender
     * @param listener consumer receiving log messages
     * @param loggerNames logger name prefixes to match
     * @return proxy implementing Appender interface
     */
    Object createAppender(String appenderName, Consumer<LogMessage> listener, List<String> loggerNames)
            throws ClassNotFoundException {
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        Class<?> loggingEventClass = classLoader.loadClass(LOGBACK_LOGGING_EVENT);

        InvocationHandler handler = new AppenderInvocationHandler(appenderName, loggingEventClass, listener, loggerNames);
        return Proxy.newProxyInstance(classLoader, new Class<?>[] {appenderClass}, handler);
    }

    /**
     * Gets the Logback LoggerContext from SLF4J.
     */
    Object getLoggerContext() throws ClassNotFoundException {
        Class<?> loggerContextClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        Object loggerContext = org.slf4j.LoggerFactory.getILoggerFactory();
        if (loggerContext == null || !loggerContextClass.isInstance(loggerContext)) {
            throw new IllegalStateException("SLF4J is not configured to use Logback as logging backend");
        }
        return loggerContext;
    }

    /**
     * Gets a logger from the LoggerContext.
     */
    Object getLogger(Object loggerContext, String loggerName) throws Exception {
        Class<?> loggerContextClass = classLoader.loadClass(LOGBACK_LOGGER_CONTEXT);
        Method getLogger = loggerContextClass.getMethod("getLogger", String.class);
        return getLogger.invoke(loggerContext, loggerName);
    }

    /**
     * Sets context on an appender.
     */
    void setContext(Object appender, Object context) throws Exception {
        Class<?> contextClass = classLoader.loadClass(LOGBACK_CONTEXT);
        Method setContext = appender.getClass().getMethod("setContext", contextClass);
        setContext.invoke(appender, context);
    }

    /**
     * Starts the appender.
     */
    void start(Object appender) throws Exception {
        Method start = appender.getClass().getMethod("start");
        start.invoke(appender);
    }

    /**
     * Stops the appender.
     */
    void stop(Object appender) throws Exception {
        Method stop = appender.getClass().getMethod("stop");
        stop.invoke(appender);
    }

    /**
     * Adds an appender to a logger.
     */
    void addAppender(Object logger, Object appender) throws Exception {
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        Method addAppender = logger.getClass().getMethod("addAppender", appenderClass);
        addAppender.invoke(logger, appender);
    }

    /**
     * Detaches an appender from a logger.
     */
    void detachAppender(Object logger, Object appender) throws Exception {
        Class<?> appenderClass = classLoader.loadClass(LOGBACK_APPENDER);
        Method detachAppender = logger.getClass().getMethod("detachAppender", appenderClass);
        detachAppender.invoke(logger, appender);
    }

    /**
     * InvocationHandler that implements Appender interface via reflection.
     */
    private static class AppenderInvocationHandler implements InvocationHandler {

        private static final String METHOD_EQUALS = "equals";
        private static final String METHOD_HASH_CODE = "hashCode";
        private static final String METHOD_TO_STRING = "toString";
        private static final String METHOD_DO_APPEND = "doAppend";
        private static final String METHOD_GET_NAME = "getName";
        private static final String METHOD_SET_NAME = "setName";
        private static final String METHOD_SET_CONTEXT = "setContext";
        private static final String METHOD_GET_CONTEXT = "getContext";
        private static final String METHOD_START = "start";
        private static final String METHOD_STOP = "stop";
        private static final String METHOD_IS_STARTED = "isStarted";

        private final String appenderName;
        private final Class<?> loggingEventClass;
        private final Consumer<LogMessage> listener;
        private final List<String> loggerNames;

        private String name;
        private boolean started = false;

        AppenderInvocationHandler(
                String appenderName,
                Class<?> loggingEventClass,
                Consumer<LogMessage> listener,
                List<String> loggerNames) {
            this.appenderName = appenderName;
            this.name = appenderName;
            this.loggingEventClass = loggingEventClass;
            this.listener = listener;
            this.loggerNames = loggerNames;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method == null) {
                return null;
            }
            String methodName = method.getName();
            switch (methodName) {
                case METHOD_EQUALS:
                    return args != null && args.length > 0 && proxy == args[0];
                case METHOD_HASH_CODE:
                    return System.identityHashCode(proxy);
                case METHOD_TO_STRING:
                    return appenderName + "-Proxy@" + Integer.toHexString(System.identityHashCode(proxy));
                case METHOD_DO_APPEND:
                    if (args != null && args.length > 0 && args[0] != null) {
                        handleLogEventSafe(args[0]);
                    }
                    return null;
                case METHOD_GET_NAME:
                    return name;
                case METHOD_SET_NAME:
                    if (args != null && args.length > 0 && args[0] instanceof String) {
                        name = (String) args[0];
                    }
                    return null;
                case METHOD_SET_CONTEXT:
                case METHOD_GET_CONTEXT:
                    return null;
                case METHOD_START:
                    started = true;
                    return null;
                case METHOD_STOP:
                    started = false;
                    return null;
                case METHOD_IS_STARTED:
                    return started;
                default:
                    return getDefaultReturnValue(method.getReturnType());
            }
        }

        private Object getDefaultReturnValue(Class<?> returnType) {
            if (returnType == null || returnType == void.class || returnType == Void.class) {
                return null;
            }
            if (returnType == boolean.class || returnType == Boolean.class) {
                return false;
            }
            if (returnType == int.class || returnType == Integer.class) {
                return 0;
            }
            if (returnType == long.class || returnType == Long.class) {
                return 0L;
            }
            return null;
        }

        private void handleLogEventSafe(Object event) {
            try {
                handleLogEvent(event);
            } catch (Exception e) {
                // Silently ignore - we don't want to disrupt logging
            }
        }

        private void handleLogEvent(Object event) throws Exception {
            Method getLoggerName = loggingEventClass.getMethod("getLoggerName");
            String loggerName = (String) getLoggerName.invoke(event);

            if (!matchesAny(loggerName)) {
                return;
            }

            Method getFormattedMessage = loggingEventClass.getMethod("getFormattedMessage");
            Method getLevel = loggingEventClass.getMethod("getLevel");
            Method getTimeStamp = loggingEventClass.getMethod("getTimeStamp");

            String message = (String) getFormattedMessage.invoke(event);
            Object level = getLevel.invoke(event);
            Long timestamp = (Long) getTimeStamp.invoke(event);

            String levelStr = level != null ? level.toString() : "UNKNOWN";
            long ts = timestamp != null ? timestamp : System.currentTimeMillis();

            LogMessage logMessage = new LogMessage(loggerName, levelStr, message, ts);
            listener.accept(logMessage);
        }

        private boolean matchesAny(String loggerName) {
            if (loggerName == null || loggerNames == null) {
                return false;
            }
            for (String prefix : loggerNames) {
                if (prefix != null && loggerName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
