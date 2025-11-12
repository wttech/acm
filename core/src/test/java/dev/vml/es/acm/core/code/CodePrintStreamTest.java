package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.LoggerContext;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Tests for CodePrintStream timestamped logging methods.
 */
class CodePrintStreamTest {

    @Test
    void shouldPrintInfoWithTimestamp() {
        // Skip test if not using Logback (e.g., in test environment with slf4j-test)
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.info("Test info message");

        String output = outputStream.toString();
        assertTrue(output.matches(".*\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[INFO\\] Test info message.*"));
    }

    @Test
    void shouldPrintErrorWithTimestamp() {
        // Skip test if not using Logback (e.g., in test environment with slf4j-test)
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.error("Test error message");

        String output = outputStream.toString();
        assertTrue(output.matches(".*\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[ERROR\\] Test error message.*"));
    }

    @Test
    void shouldPrintWarnWithTimestamp() {
        // Skip test if not using Logback (e.g., in test environment with slf4j-test)
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.warn("Test warn message");

        String output = outputStream.toString();
        assertTrue(output.matches(".*\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[WARN\\] Test warn message.*"));
    }

    @Test
    void shouldPrintDebugWithTimestamp() {
        // Skip test if not using Logback (e.g., in test environment with slf4j-test)
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.debug("Test debug message");

        String output = outputStream.toString();
        assertTrue(output.matches(".*\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[DEBUG\\] Test debug message.*"));
    }

    @Test
    void shouldPrintTraceWithTimestamp() {
        // Skip test if not using Logback (e.g., in test environment with slf4j-test)
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = new CodePrintStream(outputStream, "test-id");

        printStream.trace("Test trace message");

        String output = outputStream.toString();
        assertTrue(output.matches(".*\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[TRACE\\] Test trace message.*"));
    }

    @Test
    void shouldPrintMultipleMessagesWithDifferentLevels() {
        // Skip test if not using Logback (e.g., in test environment with slf4j-test)
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
