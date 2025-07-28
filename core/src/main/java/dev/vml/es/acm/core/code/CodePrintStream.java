package dev.vml.es.acm.core.code;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Allows writing logs simultaneously to the log files and to the code output.
 * Allows copying logs from the specified/extra loggers to the code output stream.
 */
public class CodePrintStream extends PrintStream {

    private final Logger logger;

    private final CodeLoggerPrinter loggerPrinter;

    public CodePrintStream(OutputStream output, String id) {
        super(output);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        this.loggerPrinter = new CodeLoggerPrinter(loggerContext, output);
        this.logger = loggerContext.getLogger(id);
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
        fromLoggers(CodeLoggerPrinter.NAMES);
    }

    public void fromLogger(String loggerName) {
        loggerPrinter.fromLogger(loggerName);
    }

    public void fromLoggers(String... loggerNames) {
        for (String loggerName : loggerNames) {
            fromLogger(loggerName);
        }
    }

    public void fromLoggers(List<String> loggerNames) {
        loggerNames.forEach(this::fromLogger);
    }

    @Override
    public void close() {
        loggerPrinter.disable();
        super.close();
    }

    protected Logger getLogger() {
        return logger;
    }
}
