package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

/**
 * Tests for CodePrintStream timestamped logging methods.
 */
class CodePrintStreamTest {

    private LoggerContext mockLoggerContext;
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        mockLoggerContext = mock(LoggerContext.class);
        mockLogger = mock(Logger.class);
    }

    private CodePrintStream createTestPrintStream(ByteArrayOutputStream outputStream) {
        when(mockLoggerContext.getLogger(Mockito.anyString())).thenReturn(mockLogger);

        try (MockedStatic<LoggerFactory> mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class)) {
            mockedLoggerFactory.when(LoggerFactory::getILoggerFactory).thenReturn(mockLoggerContext);
            return new CodePrintStream(outputStream, "test-id");
        }
    }

    @Test
    void shouldPrintInfoWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = createTestPrintStream(outputStream);

        printStream.info("Test info message");

        String output = outputStream.toString();
        assertTrue(
                output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[INFO\\] Test info message\\R"),
                "Output was: " + output);
    }

    @Test
    void shouldPrintErrorWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = createTestPrintStream(outputStream);

        printStream.error("Test error message");

        String output = outputStream.toString();
        assertTrue(
                output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[ERROR\\] Test error message\\R"),
                "Output was: " + output);
    }

    @Test
    void shouldPrintWarnWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = createTestPrintStream(outputStream);

        printStream.warn("Test warn message");

        String output = outputStream.toString();
        assertTrue(
                output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[WARN\\] Test warn message\\R"),
                "Output was: " + output);
    }

    @Test
    void shouldPrintDebugWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = createTestPrintStream(outputStream);

        printStream.debug("Test debug message");

        String output = outputStream.toString();
        assertTrue(
                output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[DEBUG\\] Test debug message\\R"),
                "Output was: " + output);
    }

    @Test
    void shouldPrintTraceWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = createTestPrintStream(outputStream);

        printStream.trace("Test trace message");

        String output = outputStream.toString();
        assertTrue(
                output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[TRACE\\] Test trace message\\R"),
                "Output was: " + output);
    }

    @Test
    void shouldPrintMultipleMessagesWithDifferentLevels() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodePrintStream printStream = createTestPrintStream(outputStream);

        printStream.info("Info message");
        printStream.error("Error message");
        printStream.warn("Warn message");

        String output = outputStream.toString();
        assertTrue(output.contains("[INFO] Info message"), "Output was: " + output);
        assertTrue(output.contains("[ERROR] Error message"), "Output was: " + output);
        assertTrue(output.contains("[WARN] Warn message"), "Output was: " + output);
    }
}
