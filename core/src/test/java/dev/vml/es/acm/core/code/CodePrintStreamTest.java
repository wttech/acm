package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.LoggerContext;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class CodePrintStreamTest {

    @Test
    void shouldPrintInfoWithTimestamp() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.info("Test info message");

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[INFO\\] Test info message\\R"));
    }

    @Test
    void shouldPrintErrorWithTimestamp() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.error("Test error message");

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[ERROR\\] Test error message\\R"));
    }

    @Test
    void shouldPrintWarnWithTimestamp() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.warn("Test warn message");

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[WARN\\] Test warn message\\R"));
    }

    @Test
    void shouldPrintDebugWithTimestamp() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.debug("Test debug message");

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[DEBUG\\] Test debug message\\R"));
    }

    @Test
    void shouldPrintTraceWithTimestamp() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.trace("Test trace message");

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[TRACE\\] Test trace message\\R"));
    }

    @Test
    void shouldPrintMultipleMessagesWithDifferentLevels() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.info("Info message");
        printStream.error("Error message");
        printStream.warn("Warn message");

        String output = outputStream.toString();
        assertTrue(output.contains("[INFO] Info message"));
        assertTrue(output.contains("[ERROR] Error message"));
        assertTrue(output.contains("[WARN] Warn message"));
    }
}
