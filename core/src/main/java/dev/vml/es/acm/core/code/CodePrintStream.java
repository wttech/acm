package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.log.CodePrintLogger;
import dev.vml.es.acm.core.code.log.LogInterceptor;
import dev.vml.es.acm.core.code.log.LogInterceptorManager;
import dev.vml.es.acm.core.code.log.LogMessage;
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
import org.slf4j.Logger;
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
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Logger logger;

    private final LogInterceptorManager logInterceptorManager;

    private final Set<String> loggerNames;

    private final Set<String> bridgedLoggerNames;

    private boolean loggerTimestamps;

    private LogInterceptor.Handle interceptorHandle;

    private boolean printerTimestamps;

    public CodePrintStream(OutputStream output, String id) {
        this(output, id, null);
    }

    public CodePrintStream(OutputStream output, String id, LogInterceptorManager logInterceptorManager) {
        super(output);

        this.logInterceptorManager = logInterceptorManager;
        this.loggerNames = new HashSet<>();
        this.bridgedLoggerNames = new HashSet<>();
        this.loggerTimestamps = true;
        this.logger = bridgeLogger(LoggerFactory.getLogger(id));
        this.printerTimestamps = true;
    }

    Logger bridgeLogger(Logger delegate) {
        bridgedLoggerNames.add(delegate.getName());
        return new CodePrintLogger(delegate, this::handleLogMessage);
    }

    private boolean isBridgedLogger(String loggerName) {
        return LOGGER_NAME_ACL.equals(loggerName)
                || LOGGER_NAME_REPO.equals(loggerName)
                || bridgedLoggerNames.contains(loggerName);
    }

    public void fromLogger(String loggerName) {
        if (StringUtils.isBlank(loggerName)) {
            throw new AcmException("Logger name cannot be blank!");
        }
        if (loggerNames.add(loggerName.trim())) {
            updateInterceptor();
        }
    }

    @Override
    public void close() {
        detachInterceptor();
        super.close();
    }

    private void updateInterceptor() {
        detachInterceptor();
        if (!loggerNames.isEmpty() && logInterceptorManager != null) {
            String[] externalLoggers =
                    loggerNames.stream().filter(name -> !isBridgedLogger(name)).toArray(String[]::new);
            if (externalLoggers.length > 0) {
                interceptorHandle = logInterceptorManager.attach(this::handleLogMessage, externalLoggers);
            }
        }
    }

    private void detachInterceptor() {
        LogInterceptor.Handle handle = interceptorHandle;
        if (handle != null) {
            interceptorHandle = null;
            handle.detach();
        }
    }

    private void handleLogMessage(LogMessage event) {
        String loggerName = event.getLoggerName();
        for (String loggerPrefix : loggerNames) {
            if (StringUtils.startsWith(loggerName, loggerPrefix)) {
                String level = event.getLevel();
                if (loggerTimestamps) {
                    LocalDateTime eventTime =
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), ZoneId.systemDefault());
                    String timestamp = eventTime.format(TIMESTAMP_FORMATTER);
                    println(timestamp + " [" + level + "] " + event.getMessage());
                } else {
                    println('[' + level + "] " + event.getMessage());
                }
                break;
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isLoggerTimestamps() {
        return loggerTimestamps;
    }

    public void setLoggerTimestamps(boolean flag) {
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
        if (loggerNames == null) {
            return;
        }
        boolean changed = false;
        for (String loggerName : loggerNames) {
            if (StringUtils.isNotBlank(loggerName) && this.loggerNames.add(loggerName.trim())) {
                changed = true;
            }
        }
        if (changed) {
            updateInterceptor();
        }
    }

    public void fromLoggers(List<String> loggerNames) {
        if (loggerNames != null) {
            fromLoggers(loggerNames.toArray(new String[0]));
        }
    }

    public void setPrinterTimestamps(boolean flag) {
        this.printerTimestamps = flag;
    }

    public boolean isPrinterTimestamps() {
        return printerTimestamps;
    }

    public void printTimestamped(String level, String message) {
        printTimestamped(CodePrintLevel.of(level), message);
    }

    public void printTimestamped(CodePrintLevel level, String message) {
        if (printerTimestamps) {
            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(TIMESTAMP_FORMATTER);
            println(timestamp + " [" + level + "] " + message);
        } else {
            println("[" + level + "] " + message);
        }
    }

    public void success(String message) {
        printTimestamped(CodePrintLevel.SUCCESS, message);
    }

    public void info(String message) {
        printTimestamped(CodePrintLevel.INFO, message);
    }

    public void error(String message) {
        printTimestamped(CodePrintLevel.ERROR, message);
    }

    public void warn(String message) {
        printTimestamped(CodePrintLevel.WARN, message);
    }

    public void debug(String message) {
        printTimestamped(CodePrintLevel.DEBUG, message);
    }
}
