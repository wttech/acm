package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vml.es.acm.core.code.log.LogInterceptor;
import dev.vml.es.acm.core.code.log.LogInterceptorManager;
import dev.vml.es.acm.core.code.log.LogMessage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Test
    void shouldBridgeSelfAndRecommendedLoggersWithoutInterceptor() {
        List<String> interceptedLoggers = new ArrayList<>();
        LogInterceptorManager manager = new LogInterceptorManager() {
            @Override
            public LogInterceptor.Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
                interceptedLoggers.addAll(Arrays.asList(loggerNames));
                return () -> {};
            }
        };

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "console|1", manager)) {
            Logger repoLogger = out.bridgeLogger(LoggerFactory.getLogger(CodePrintStream.LOGGER_NAME_REPO));
            out.fromSelfLogger();
            out.fromRecommendedLoggers();

            out.getLogger().info("Self message {}", 1);
            repoLogger.info("Repo message {}", 2);
        }

        assertTrue(interceptedLoggers.isEmpty());
        String output = outputStream.toString();
        assertTrue(output.contains("[INFO] Self message 1"));
        assertTrue(output.contains("[INFO] Repo message 2"));
    }

    @Test
    void shouldKeepOutputEmptyWhenLogPrintingNotEnabled() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "console|1", null)) {
            Logger repoLogger = out.bridgeLogger(LoggerFactory.getLogger(CodePrintStream.LOGGER_NAME_REPO));
            out.getLogger().info("Should not be printed");
            repoLogger.info("Should not be printed either");
        }

        assertEquals("", outputStream.toString());
    }

    @Test
    void shouldAttachInterceptorOnlyForExternalLoggers() {
        List<String> interceptedLoggers = new ArrayList<>();
        LogInterceptorManager manager = new LogInterceptorManager() {
            @Override
            public LogInterceptor.Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
                interceptedLoggers.addAll(Arrays.asList(loggerNames));
                return () -> {};
            }
        };

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CodePrintStream out = new CodePrintStream(outputStream, "console|1", manager)) {
            out.fromSelfLogger();
            out.fromRecommendedLoggers();
            out.fromLogger("com.acme.custom");
        }

        assertEquals(Arrays.asList("com.acme.custom"), interceptedLoggers);
    }
}
