package dev.vml.es.acm.core.code;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import dev.vml.es.acm.core.AcmException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Allows writing logs simultaneously to the log files and to the code output.
 * Allows copying logs from the specified/extra loggers to the code output stream.
 */
public class CodePrintStream extends PrintStream {

    public static final String LOGGER_NAME_ACL = "dev.vml.es.acm.core.acl";
    public static final String LOGGER_NAME_REPO = "dev.vml.es.acm.core.repo";
    public static final String[] LOGGER_NAMES = {LOGGER_NAME_ACL, LOGGER_NAME_REPO};

    // have to match pattern in 'monaco/log.ts'
    private static final DateTimeFormatter LOGGER_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Logger logger;

    private final LoggerContext loggerContext;

    private final Set<String> loggerNames;

    private boolean loggerTimestamps;

    private final LogAppender logAppender;

    public CodePrintStream(OutputStream output, String id) {
        super(output);

        this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        this.loggerNames = new HashSet<>();
        this.loggerTimestamps = true;
        this.logger = loggerContext.getLogger(id);
        this.logAppender = new LogAppender();
    }

    private class LogAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            String loggerName = event.getLoggerName();
            for (String loggerPrefix : loggerNames) {
                if (StringUtils.startsWith(loggerName, loggerPrefix)) {
                    String level = event.getLevel().toString();
                    if (loggerTimestamps) {
                        LocalDateTime eventTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());
                        String timestamp = eventTime.format(LOGGER_TIMESTAMP_FORMATTER);
                        println(timestamp + " [" + level + "] " + event.getFormattedMessage());
                    } else {
                        println('[' + level + "] " + event.getFormattedMessage());
                    }
                    break;
                }
            }
        }
    }

    public void fromLogger(String loggerName) {
        if (StringUtils.isBlank(loggerName)) {
            throw new AcmException("Logger name cannot be blank!");
        }
        enableAppender();
        loggerNames.add(loggerName);
    }

    @Override
    public void close() {
        disableAppender();
        super.close();
    }

    private void enableAppender() {
        if (logAppender.isStarted()) {
            return;
        }
        Logger rootLogger = getRootLogger();
        rootLogger.addAppender(logAppender);
        logAppender.setContext(loggerContext);
        logAppender.start();
    }

    private void disableAppender() {
        if (!logAppender.isStarted()) {
            return;
        }
        logAppender.stop();
        Logger rootLogger = getRootLogger();
        rootLogger.detachAppender(logAppender);
    }

    private Logger getRootLogger() {
        return loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isLoggerTimestamps() {
        return loggerTimestamps;
    }

    public void withLoggerTimestamps(boolean flag) {
        this.loggerTimestamps = flag;
    }

    public void fromLogs() {
        fromLoggers();
    }

    public void fromLoggers() {
        fromSelfLogger();
        fromRecommendedLoggers();
    }

    public void fromSelfLogger() {
        fromLogger(logger.getName());
    }

    public void fromRecommendedLoggers() {
        fromLoggers(LOGGER_NAMES);
    }

    public void fromLoggers(String... loggerNames) {
        for (String loggerName : loggerNames) {
            fromLogger(loggerName);
        }
    }

    public void fromLoggers(List<String> loggerNames) {
        loggerNames.forEach(this::fromLogger);
    }

    public void info(String message) {
        printStamped(CodePrintLevel.INFO, message);
    }

    public void error(String message) {
        printStamped(CodePrintLevel.ERROR, message);
    }

    public void warn(String message) {
        printStamped(CodePrintLevel.WARN, message);
    }

    public void debug(String message) {
        printStamped(CodePrintLevel.DEBUG, message);
    }

    public void trace(String message) {
        printStamped(CodePrintLevel.TRACE, message);
    }

    public void printStamped(String level, String message) {
        printStamped(CodePrintLevel.of(level), message);
    }

    public void printStamped(CodePrintLevel level, String message) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(LOGGER_TIMESTAMP_FORMATTER);
        println(timestamp + " [" + level + "] " + message);
    }
}
