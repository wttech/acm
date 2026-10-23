package dev.vml.es.acm.core.code.log;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * SLF4J {@link Logger} decorator that forwards log messages directly to a {@link LogMessage} listener
 * in addition to the underlying SLF4J logger, without relying on Logback internals or reflection.
 */
public class CodePrintLogger implements Logger {

    private static final String LEVEL_TRACE = "TRACE";
    private static final String LEVEL_DEBUG = "DEBUG";
    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_WARN = "WARN";
    private static final String LEVEL_ERROR = "ERROR";

    private final Logger delegate;
    private final Consumer<LogMessage> listener;

    public CodePrintLogger(Logger delegate, Consumer<LogMessage> listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    private void emit(String level, String message) {
        if (listener != null) {
            listener.accept(new LogMessage(
                    delegate.getName(), level, message != null ? message : "", System.currentTimeMillis()));
        }
    }

    private void emit(String level, String format, Object arg) {
        emit(level, MessageFormatter.format(format, arg).getMessage());
    }

    private void emit(String level, String format, Object arg1, Object arg2) {
        emit(level, MessageFormatter.format(format, arg1, arg2).getMessage());
    }

    private void emit(String level, String format, Object[] arguments) {
        emit(level, MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        delegate.trace(msg);
        if (delegate.isTraceEnabled()) {
            emit(LEVEL_TRACE, msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.trace(format, arg);
        if (delegate.isTraceEnabled()) {
            emit(LEVEL_TRACE, format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.trace(format, arg1, arg2);
        if (delegate.isTraceEnabled()) {
            emit(LEVEL_TRACE, format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.trace(format, arguments);
        if (delegate.isTraceEnabled()) {
            emit(LEVEL_TRACE, format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.trace(msg, t);
        if (delegate.isTraceEnabled()) {
            emit(LEVEL_TRACE, msg);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        delegate.trace(marker, msg);
        if (delegate.isTraceEnabled(marker)) {
            emit(LEVEL_TRACE, msg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        delegate.trace(marker, format, arg);
        if (delegate.isTraceEnabled(marker)) {
            emit(LEVEL_TRACE, format, arg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        delegate.trace(marker, format, arg1, arg2);
        if (delegate.isTraceEnabled(marker)) {
            emit(LEVEL_TRACE, format, arg1, arg2);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        delegate.trace(marker, format, argArray);
        if (delegate.isTraceEnabled(marker)) {
            emit(LEVEL_TRACE, format, argArray);
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        delegate.trace(marker, msg, t);
        if (delegate.isTraceEnabled(marker)) {
            emit(LEVEL_TRACE, msg);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        delegate.debug(msg);
        if (delegate.isDebugEnabled()) {
            emit(LEVEL_DEBUG, msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        delegate.debug(format, arg);
        if (delegate.isDebugEnabled()) {
            emit(LEVEL_DEBUG, format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        delegate.debug(format, arg1, arg2);
        if (delegate.isDebugEnabled()) {
            emit(LEVEL_DEBUG, format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        delegate.debug(format, arguments);
        if (delegate.isDebugEnabled()) {
            emit(LEVEL_DEBUG, format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.debug(msg, t);
        if (delegate.isDebugEnabled()) {
            emit(LEVEL_DEBUG, msg);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        delegate.debug(marker, msg);
        if (delegate.isDebugEnabled(marker)) {
            emit(LEVEL_DEBUG, msg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        delegate.debug(marker, format, arg);
        if (delegate.isDebugEnabled(marker)) {
            emit(LEVEL_DEBUG, format, arg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        delegate.debug(marker, format, arg1, arg2);
        if (delegate.isDebugEnabled(marker)) {
            emit(LEVEL_DEBUG, format, arg1, arg2);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        delegate.debug(marker, format, arguments);
        if (delegate.isDebugEnabled(marker)) {
            emit(LEVEL_DEBUG, format, arguments);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        delegate.debug(marker, msg, t);
        if (delegate.isDebugEnabled(marker)) {
            emit(LEVEL_DEBUG, msg);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        delegate.info(msg);
        emit(LEVEL_INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        delegate.info(format, arg);
        emit(LEVEL_INFO, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
        emit(LEVEL_INFO, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        delegate.info(format, arguments);
        emit(LEVEL_INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        delegate.info(msg, t);
        emit(LEVEL_INFO, msg);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        delegate.info(marker, msg);
        emit(LEVEL_INFO, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        delegate.info(marker, format, arg);
        emit(LEVEL_INFO, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        delegate.info(marker, format, arg1, arg2);
        emit(LEVEL_INFO, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        delegate.info(marker, format, arguments);
        emit(LEVEL_INFO, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        delegate.info(marker, msg, t);
        emit(LEVEL_INFO, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        delegate.warn(msg);
        emit(LEVEL_WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        delegate.warn(format, arg);
        emit(LEVEL_WARN, format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        delegate.warn(format, arguments);
        emit(LEVEL_WARN, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        delegate.warn(format, arg1, arg2);
        emit(LEVEL_WARN, format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        delegate.warn(msg, t);
        emit(LEVEL_WARN, msg);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        delegate.warn(marker, msg);
        emit(LEVEL_WARN, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        delegate.warn(marker, format, arg);
        emit(LEVEL_WARN, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        delegate.warn(marker, format, arg1, arg2);
        emit(LEVEL_WARN, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        delegate.warn(marker, format, arguments);
        emit(LEVEL_WARN, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        delegate.warn(marker, msg, t);
        emit(LEVEL_WARN, msg);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        delegate.error(msg);
        emit(LEVEL_ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        delegate.error(format, arg);
        emit(LEVEL_ERROR, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        delegate.error(format, arg1, arg2);
        emit(LEVEL_ERROR, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        delegate.error(format, arguments);
        emit(LEVEL_ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        delegate.error(msg, t);
        emit(LEVEL_ERROR, msg);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        delegate.error(marker, msg);
        emit(LEVEL_ERROR, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        delegate.error(marker, format, arg);
        emit(LEVEL_ERROR, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(marker, format, arg1, arg2);
        emit(LEVEL_ERROR, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        delegate.error(marker, format, arguments);
        emit(LEVEL_ERROR, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        delegate.error(marker, msg, t);
        emit(LEVEL_ERROR, msg);
    }
}
