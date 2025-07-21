package dev.vml.es.acm.core.code;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;

/**
 * Allows writing logs simultaneously to the log files and to the code output.
 * Allows copying logs from the specified/extra loggers to the code output stream.
 */
public class CodePrintStream extends PrintStream {

    private final String id;

    private final Logger selfLogger;

    private final List<Logger> registeredLoggers;

    private static LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    public CodePrintStream(String id, OutputStream output) {
        super(output);
        this.id = id;
        this.selfLogger = getLoggerContext().getLogger(String.format("%s(%s)", getClass().getName(), id));
        this.registeredLoggers = new CopyOnWriteArrayList<>();
    }

    public String getAppenderName() {
        return id;
    }

    public void fromLogs() {
        fromSelfLogs();
        fromRepoLogs();
        fromAclLogs();
    }

    private void fromSelfLogs() {
        fromLogger(selfLogger);
    }

    public void fromAclLogs() {
        fromLogger("dev.vml.es.acm.core.acl");
    }

    public void fromRepoLogs() {
        fromLogger("dev.vml.es.acm.core.repo");
    }

    public void fromLogger(String loggerName) {
                getLoggerContext().getLoggerList().stream()
                .filter(logger -> logger.getName().contains(loggerName))
                .filter(logger -> logger.getAppender(getAppenderName()) == null)
                .forEach(this::fromLogger);
    }

    public void fromLogger(Logger logger) {
        if (logger.getAppender(getAppenderName()) == null && !registeredLoggers.contains(logger)) {
            registerLogger(logger);
        }
    }

    private void registerLogger(Logger logger) {
        registeredLoggers.add(logger);

        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setName(getAppenderName());
        appender.setContext(getLoggerContext());
        appender.setOutputStream(this);

        PatternLayout layout = new PatternLayout();
        layout.setContext(getLoggerContext());
        layout.setPattern("%msg%n");
        layout.start();

        appender.setLayout(layout);
        appender.start();

        logger.addAppender(appender);
        logger.setAdditive(true);
    }

    private void unregisterLoggers() {
        registeredLoggers.forEach(logger -> logger.detachAppender(getAppenderName()));
        registeredLoggers.clear();
    }

    @Override
    public void close() {
        unregisterLoggers();
        super.close();
    }

    public Logger getLogger() {
        return selfLogger;
    }
}
