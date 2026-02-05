package dev.vml.es.acm.core.code.log;

import java.util.function.Consumer;

/**
 * Intercepts log messages from specified loggers for automatic capture in script output.
 *
 * <p>Opt-in feature controlled by {@link dev.vml.es.acm.core.code.Executor.Config#logPrintingEnabled()}.
 * Scripts can always log manually via SLF4J regardless of this setting.</p>
 */
public interface LogInterceptor {

    boolean isAvailable();

    Handle attach(Consumer<LogMessage> listener, String... loggerNames);

    @FunctionalInterface
    interface Handle {
        void detach();
    }
}
