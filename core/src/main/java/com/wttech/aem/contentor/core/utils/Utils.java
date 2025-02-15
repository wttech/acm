package com.wttech.aem.contentor.core.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import java.io.OutputStream;
import org.slf4j.LoggerFactory;

public class Utils {

    private final OutputStreamWrapper out;

    public Utils(OutputStream out) {
        this.out = (OutputStreamWrapper) out;
    }

    public void redirectLogToOutput(String loggerName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLoggerList().stream()
                .filter(logger -> logger.getName().contains(loggerName))
                .forEach(logger -> {
                    out.registerLogger(logger);

                    OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
                    appender.setName(out.getAppenderName());
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
    }
}
