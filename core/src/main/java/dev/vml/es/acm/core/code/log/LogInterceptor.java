package dev.vml.es.acm.core.code.log;

import java.util.function.Consumer;

/**
 * Intercepts log messages from specified loggers and forwards them to a consumer.
 * Implementations may use different underlying logging frameworks.
 */
public interface LogInterceptor {

    /**
     * Checks if this interceptor is available on the current AEM instance.
     */
    boolean isAvailable();

    /**
     * Attaches the interceptor to capture log messages from specified loggers.
     *
     * @param listener consumer that receives log events
     * @param loggerNames logger names (or prefixes) to intercept
     * @return handle to detach the interceptor when done
     */
    Handle attach(Consumer<LogMessage> listener, String... loggerNames);

    /**
     * Handle to detach the log interceptor.
     */
    @FunctionalInterface
    interface Handle {
        void detach();
    }
}
