package dev.vml.es.acm.core.code;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class CodeLoggerPrinter extends AppenderBase<ILoggingEvent> {

    private final LoggerContext loggerContext;

    private final Set<String> loggerNames;

    private final PrintStream printStream;

    public CodeLoggerPrinter(LoggerContext loggerContext, OutputStream outputStream) {
        this.loggerContext = loggerContext;
        this.loggerNames = new HashSet<>();
        this.printStream = new PrintStream(outputStream);
    }

    public void fromLogger(String name) {
        enable();
        loggerNames.add(name);
    }

    public void enable() {
        if (isStarted()) {
            return;
        }
        Logger rootLogger = getRootLogger();
        rootLogger.addAppender(this);
        setContext(loggerContext);
        start();
    }

    public void disable() {
        if (!isStarted()) {
            return;
        }
        stop();
        Logger rootLogger = getRootLogger();
        rootLogger.detachAppender(this);
    }

    private Logger getRootLogger() {
        return loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    @Override
    protected void append(ILoggingEvent event) {
        String loggerName = event.getLoggerName();
        for (String loggerPrefix : loggerNames) {
            if (StringUtils.startsWith(loggerName, loggerPrefix)) {
                printStream.println(event.getFormattedMessage());
                break;
            }
        }
    }
}