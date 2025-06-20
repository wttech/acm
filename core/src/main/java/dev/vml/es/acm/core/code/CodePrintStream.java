package dev.vml.es.acm.core.code;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

public class CodePrintStream extends PrintStream {

    private final String id;

    private final List<Logger> loggers;

    public CodePrintStream(String id, OutputStream output) {
        super(output);
        this.id = id;
        this.loggers = new LinkedList<>();
    }

    public String getAppenderName() {
        return id;
    }

    public void registerLogger(Logger logger) {
        loggers.add(logger);
    }

    @Override
    public void close() {
        loggers.forEach(logger -> logger.detachAppender(getAppenderName()));
        super.close();
    }

    public void fromLogs() {
        fromRepoLogs();
        fromAclLogs();
    }

    public void fromAclLogs() {
        fromLogger("dev.vml.es.acm.core.acl");
    }

    public void fromRepoLogs() {
        fromLogger("dev.vml.es.acm.core.repo");
    }

    public List<String> fromLogger(String loggerName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggers = loggerContext.getLoggerList().stream()
                .filter(logger -> logger.getName().contains(loggerName))
                .filter(logger -> logger.getAppender(getAppenderName()) == null)
                .collect(Collectors.toList());
        loggers.forEach(logger -> {
            registerLogger(logger);

            OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
            appender.setName(getAppenderName());
            appender.setContext(loggerContext);
            appender.setOutputStream(this);

            PatternLayout layout = new PatternLayout();
            layout.setContext(loggerContext);
            layout.setPattern("%msg%n");
            layout.start();

            appender.setLayout(layout);
            appender.start();

            logger.addAppender(appender);
            logger.setAdditive(false);
        });
        return loggers.stream().map(Logger::getName).collect(Collectors.toList());
    }
}
