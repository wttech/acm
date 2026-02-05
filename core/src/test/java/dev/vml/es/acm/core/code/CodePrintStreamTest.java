package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class CodePrintStreamTest {

    @Test
    void shouldPrintInfoWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "test-id", null)) {
            out.info("Test info message");
        }

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[INFO\\] Test info message\\R"));
    }

    @Test
    void shouldPrintErrorWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "test-id", null)) {
            out.error("Test error message");
        }

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[ERROR\\] Test error message\\R"));
    }

    @Test
    void shouldPrintWarnWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "test-id", null)) {
            out.warn("Test warn message");
        }

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[WARN\\] Test warn message\\R"));
    }

    @Test
    void shouldPrintSuccessWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "test-id", null)) {
            out.success("Test success message");
        }

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[SUCCESS\\] Test success message\\R"));
    }

    @Test
    void shouldPrintDebugWithTimestamp() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "test-id", null)) {
            out.debug("Test debug message");
        }

        String output = outputStream.toString();
        assertTrue(output.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[DEBUG\\] Test debug message\\R"));
    }

    @Test
    void shouldPrintMultipleMessagesWithDifferentLevels() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "test-id", null)) {
            out.info("Info message");
            out.error("Error message");
            out.warn("Warn message");
            out.debug("Debug message");
            out.success("Success message");
        }

        String output = outputStream.toString();
        assertTrue(output.contains("[INFO] Info message"));
        assertTrue(output.contains("[ERROR] Error message"));
        assertTrue(output.contains("[WARN] Warn message"));
        assertTrue(output.contains("[DEBUG] Debug message"));
        assertTrue(output.contains("[SUCCESS] Success message"));
    }
}
