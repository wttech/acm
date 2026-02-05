package dev.vml.es.acm.core.code.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    private static final String LOGBACK_LOGGER_CONTEXT = "ch.qos.logback.classic.LoggerContext";
    private static final String LOGBACK_LOGGER = "ch.qos.logback.classic.Logger";
    private static final String LOGBACK_APPENDER = "ch.qos.logback.core.Appender";
    private static final String LOGBACK_CONTEXT = "ch.qos.logback.core.Context";
    private static final String LOGBACK_LOGGING_EVENT = "ch.qos.logback.classic.spi.ILoggingEvent";

    @Reference
    private DynamicClassLoaderManager classLoaderManager;

    @Override
    public boolean isAvailable() {
        try {
            ClassLoader cl = classLoaderManager.getDynamicClassLoader();
            cl.loadClass(LOGBACK_LOGGER_CONTEXT);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (!isAvailable()) {
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
        Class<?> loggingEventClass = cl.loadClass(LOGBACK_LOGGING_EVENT);

        Object loggerContext = LoggerFactory.getILoggerFactory();
        Method getLogger = loggerContextClass.getMethod("getLogger", String.class);
        Method addAppender = loggerClass.getMethod("addAppender", appenderClass);
        Method detachAppender = loggerClass.getMethod("detachAppender", appenderClass);

        Object appender = createAppenderProxy(cl, appenderClass, loggingEventClass, listener, loggerNames);

        // Attach to root logger
        Object rootLogger = getLogger.invoke(loggerContext, "ROOT");
        invokeMethod(appender, "setContext", new Class<?>[] {cl.loadClass(LOGBACK_CONTEXT)}, loggerContext);
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
            String[] loggerNames) {
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

        private final Class<?> loggingEventClass;
        private final Consumer<LogMessage> listener;
        private final String[] loggerNames;
        private String name = "ACM-LogInterceptor";
        private boolean started = false;

        AppenderInvocationHandler(Class<?> loggingEventClass, Consumer<LogMessage> listener, String[] loggerNames) {
            this.loggingEventClass = loggingEventClass;
            this.listener = listener;
            this.loggerNames = loggerNames;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            switch (methodName) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "ACM-LogInterceptor-Proxy@" + Integer.toHexString(System.identityHashCode(proxy));
                case "doAppend":
                    if (args != null && args.length > 0) {
                        handleLogEvent(args[0]);
                    }
                    return null;
                case "getName":
                    return name;
                case "setName":
                    if (args != null && args.length > 0) {
                        name = (String) args[0];
                    }
                    return null;
                case "setContext":
                    return null;
                case "getContext":
                    return null;
                case "start":
                    started = true;
                    return null;
                case "stop":
                    started = false;
                    return null;
                case "isStarted":
                    return started;
                case "addFilter":
                case "clearAllFilters":
                case "getCopyOfAttachedFiltersList":
                case "getFilterChainDecision":
                    return null;
                default:
                    return null;
            }
        }

        private void handleLogEvent(Object event) throws Exception {
            String loggerName =
                    (String) loggingEventClass.getMethod("getLoggerName").invoke(event);

            if (!matchesAny(loggerName, loggerNames)) {
                return;
            }

            String message =
                    (String) loggingEventClass.getMethod("getFormattedMessage").invoke(event);
            Object level = loggingEventClass.getMethod("getLevel").invoke(event);
            Long timestamp = (Long) loggingEventClass.getMethod("getTimeStamp").invoke(event);

            LogMessage logMessage = new LogMessage(loggerName, level.toString(), message, timestamp);
            listener.accept(logMessage);
        }

        private boolean matchesAny(String loggerName, String[] prefixes) {
            if (loggerName == null) {
                return false;
            }
            for (String prefix : prefixes) {
                if (loggerName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
