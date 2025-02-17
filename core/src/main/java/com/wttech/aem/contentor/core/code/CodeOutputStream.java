package com.wttech.aem.contentor.core.code;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

public class CodeOutputStream extends OutputStream {

    private final ExecutionContext context;

    private final OutputStream out;

    private final List<Logger> loggers;

    public CodeOutputStream(ExecutionContext context) throws IOException {
        this.context = context;
        this.out = Files.newOutputStream(ExecutionOutput.path(context.getId()));
        this.loggers = new ArrayList<>();
    }

    public String getAppenderName() {
        return context.getId();
    }

    public void registerLogger(Logger logger) {
        loggers.add(logger);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        loggers.forEach(logger -> logger.detachAppender(getAppenderName()));
        out.close();
    }

    public void fromLogs() {
        fromAclLogs();
    }

    public void fromAclLogs() {
        fromLogger("com.wttech.aem.contentor.core.acl");
    }

    // TODO register logger like on http://localhost:4502/system/console/slinglog
    public List<String> fromLogger(String loggerName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Logger> loggers = context.getLoggerList().stream()
                .filter(logger -> logger.getName().contains(loggerName))
                .filter(logger -> logger.getAppender(getAppenderName()) == null)
                .collect(Collectors.toList());
        loggers.forEach(logger -> {
            registerLogger(logger);

            OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
            appender.setName(getAppenderName());
            appender.setContext(context);
            appender.setOutputStream(out);

            PatternLayout layout = new PatternLayout();
            layout.setContext(context);
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
