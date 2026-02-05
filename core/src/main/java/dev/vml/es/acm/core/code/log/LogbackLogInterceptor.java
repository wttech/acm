package dev.vml.es.acm.core.code.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log interceptor using Logback API via reflection.
 * Fallback for AEM 6.5.x where OSGi Log Service 1.4 is not available.
 */
@Component(service = LogInterceptor.class, property = "type=" + LogInterceptor.TYPE_LOGBACK)
public class LogbackLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LogbackLogInterceptor.class);

    private static final String APPENDER_NAME = "ACM-LogInterceptor";

    private static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";
    private static final String LOGBACK_LOGGER = "ch.qos.logback.classic.Logger";
    private static final String LOGBACK_APPENDER = "ch.qos.logback.core.Appender";
    private static final String LOGBACK_CONTEXT = "ch.qos.logback.core.Context";
    private static final String LOGBACK_LOGGING_EVENT = "ch.qos.logback.classic.spi.ILoggingEvent";
    private static final String LOGBACK_ROOT_LOGGER = "ROOT";

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
            cl.loadClass(LOGBACK_LOGGER_CONTEXT);
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
            LOG.debug("Invalid parameters for attach: listener={}, loggerNames={}", listener, loggerNames);
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.debug("Logback is not available");
            return () -> {};
        }

        try {
            return doAttach(listener, loggerNames);
        } catch (Exception e) {
            LOG.warn("Failed to attach Logback interceptor", e);
            return () -> {};
        }
    }

    private Handle doAttach(Consumer<LogMessage> listener, String[] loggerNames) throws Exception {
        ClassLoader cl = classLoaderManager.getDynamicClassLoader();

        Class<?> loggerContextClass = cl.loadClass(LOGBACK_LOGGER_CONTEXT);
        Class<?> loggerClass = cl.loadClass(LOGBACK_LOGGER);
        Class<?> appenderClass = cl.loadClass(LOGBACK_APPENDER);
        Class<?> contextClass = cl.loadClass(LOGBACK_CONTEXT);
        Class<?> loggingEventClass = cl.loadClass(LOGBACK_LOGGING_EVENT);

        Object loggerContext = LoggerFactory.getILoggerFactory();
        if (loggerContext == null || !loggerContextClass.isInstance(loggerContext)) {
            throw new IllegalStateException("SLF4J is not configured to use Logback as logging backend");
        }

        Method getLogger = loggerContextClass.getMethod("getLogger", String.class);
        Method addAppender = loggerClass.getMethod("addAppender", appenderClass);
        Method detachAppender = loggerClass.getMethod("detachAppender", appenderClass);

        List<String> loggerNameList = Collections.unmodifiableList(Arrays.asList(loggerNames));
        Object appender = createAppenderProxy(cl, appenderClass, loggingEventClass, listener, loggerNameList);

        Object rootLogger = getLogger.invoke(loggerContext, LOGBACK_ROOT_LOGGER);
        if (rootLogger == null) {
            throw new IllegalStateException("Could not obtain root logger from logging context");
        }

        invokeMethod(appender, "setContext", new Class<?>[] {contextClass}, loggerContext);
        invokeMethod(appender, "start");
        addAppender.invoke(rootLogger, appender);

        return () -> {
            try {
                invokeMethod(appender, "stop");
                detachAppender.invoke(rootLogger, appender);
            } catch (Exception e) {
                LOG.debug("Failed to detach Logback appender", e);
            }
        };
    }

    private Object createAppenderProxy(
            ClassLoader cl,
            Class<?> appenderClass,
            Class<?> loggingEventClass,
            Consumer<LogMessage> listener,
            List<String> loggerNames) {
        InvocationHandler handler = new AppenderInvocationHandler(loggingEventClass, listener, loggerNames);
        return Proxy.newProxyInstance(cl, new Class<?>[] {appenderClass}, handler);
    }

    private void invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getMethod(methodName, paramTypes);
        method.invoke(target, args);
    }

    private void invokeMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        method.invoke(target);
    }

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

        private final Class<?> loggingEventClass;
        private final Consumer<LogMessage> listener;
        private final List<String> loggerNames;

        private String name = APPENDER_NAME;
        private boolean started = false;

        AppenderInvocationHandler(Class<?> loggingEventClass, Consumer<LogMessage> listener, List<String> loggerNames) {
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
                    return APPENDER_NAME + "-Proxy@" + Integer.toHexString(System.identityHashCode(proxy));
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
                    // Handle any other Appender interface methods gracefully
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
