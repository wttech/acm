package dev.vml.es.acm.core.code.log;

public class LogMessage {

    private final String loggerName;

    private final String level;

    private final String message;

    private final long timestamp;

    public LogMessage(String loggerName, String level, String message, long timestamp) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
