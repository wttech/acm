package dev.vml.es.acm.core.instance;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.BundleContext;

class ShutdownSafetyTest {

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    @TempDir
    Path tempDir;

    @Test
    void shouldBeDisabledByDefault() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        assertFalse(shutdownSafety.isEnabled());
    }

    @Test
    void shouldBeEnabledWhenConfigured() {
        ShutdownSafety shutdownSafety = activate(enabled());

        assertTrue(shutdownSafety.isEnabled());
    }

    @Test
    void shouldFollowConfigurationChangesWithoutRestart() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        MockOsgi.modified(shutdownSafety, bundleContext, enabled());

        assertTrue(shutdownSafety.isEnabled());
    }

    @Test
    void shouldAwaitSystemHealthChecksByDefault() {
        ShutdownSafety shutdownSafety = activate(enabled());

        assertArrayEquals(new String[] {"systemhealth"}, shutdownSafety.getHealthCheckTags());
    }

    @Test
    void shouldUseConfiguredHealthCheckTags() {
        Map<String, Object> props = enabled();
        props.put("healthCheckTags", new String[] {"systemhealth", "custom"});
        ShutdownSafety shutdownSafety = activate(props);

        assertArrayEquals(new String[] {"systemhealth", "custom"}, shutdownSafety.getHealthCheckTags());
    }

    @Test
    void shouldSeeExistingShutdownMarkerFileWhenEnabled() throws Exception {
        Path marker = Files.createFile(tempDir.resolve("shutdown"));
        Map<String, Object> props = enabled();
        props.put("shutdownMarkerFile", marker.toString());
        ShutdownSafety shutdownSafety = activate(props);

        assertEquals(ShutdownMarkerFile.State.EXISTS, shutdownSafety.checkShutdownMarkerFile());
    }

    @Test
    void shouldIgnoreShutdownMarkerFileWhenDisabled() throws Exception {
        Path marker = Files.createFile(tempDir.resolve("shutdown"));
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("shutdownMarkerFile", marker.toString()));

        assertEquals(ShutdownMarkerFile.State.ABSENT, shutdownSafety.checkShutdownMarkerFile());
    }

    @Test
    void shouldNotWatchShutdownMarkerFileByDefault() {
        ShutdownSafety shutdownSafety = activate(enabled());

        assertEquals(ShutdownMarkerFile.State.ABSENT, shutdownSafety.checkShutdownMarkerFile());
    }

    @Test
    void shouldTreatBlankShutdownMarkerFileAsNotSet() {
        Map<String, Object> props = enabled();
        props.put("shutdownMarkerFile", "   ");
        ShutdownSafety shutdownSafety = activate(props);

        assertEquals(ShutdownMarkerFile.State.ABSENT, shutdownSafety.checkShutdownMarkerFile());
    }

    @Test
    void shouldTreatRunAsDeadAfterThirtySecondsWithoutRefreshByDefault() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        assertEquals(30_000L, shutdownSafety.getLockAliveTimeout());
    }

    @Test
    void shouldUseConfiguredLockAliveTimeout() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("lockAliveTimeout", 60_000L));

        assertEquals(60_000L, shutdownSafety.getLockAliveTimeout());
    }

    @Test
    void shouldRaiseTooShortLockAliveTimeoutToMinimum() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("lockAliveTimeout", 1_000L));

        assertEquals(6_000L, shutdownSafety.getLockAliveTimeout());
    }

    @Test
    void shouldAllowThreeAttemptsByDefault() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        assertEquals(3L, shutdownSafety.getLockAttemptLimit());
    }

    @Test
    void shouldUseConfiguredLockAttemptLimit() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("lockAttemptLimit", 4L));

        assertEquals(4L, shutdownSafety.getLockAttemptLimit());
    }

    @Test
    void shouldRaiseTooLowLockAttemptLimitToMinimum() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("lockAttemptLimit", 1L));

        assertEquals(2L, shutdownSafety.getLockAttemptLimit());
    }

    private Map<String, Object> enabled() {
        Map<String, Object> props = new HashMap<>();
        props.put("enabled", true);
        return props;
    }

    private ShutdownSafety activate(Map<String, Object> props) {
        ShutdownSafety shutdownSafety = new ShutdownSafety();
        MockOsgi.activate(shutdownSafety, bundleContext, props);
        return shutdownSafety;
    }
}
