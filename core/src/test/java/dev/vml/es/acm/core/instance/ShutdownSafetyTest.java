package dev.vml.es.acm.core.instance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

class ShutdownSafetyTest {

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    @Test
    void shouldHaveAllTogglesOffByDefault() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        assertFalse(shutdownSafety.isAbortOnShutdown());
        assertFalse(shutdownSafety.isKeepCronUntilHealthy());
        assertFalse(shutdownSafety.isBootOnInstanceLeft());
    }

    @Test
    void shouldTurnOnOnlyAbortOnShutdown() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("abortOnShutdown", true));

        assertTrue(shutdownSafety.isAbortOnShutdown());
        assertFalse(shutdownSafety.isKeepCronUntilHealthy());
        assertFalse(shutdownSafety.isBootOnInstanceLeft());
    }

    @Test
    void shouldTurnOnOnlyKeepCronUntilHealthy() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("keepCronUntilHealthy", true));

        assertFalse(shutdownSafety.isAbortOnShutdown());
        assertTrue(shutdownSafety.isKeepCronUntilHealthy());
        assertFalse(shutdownSafety.isBootOnInstanceLeft());
    }

    @Test
    void shouldTurnOnOnlyBootOnInstanceLeft() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("bootOnInstanceLeft", true));

        assertFalse(shutdownSafety.isAbortOnShutdown());
        assertFalse(shutdownSafety.isKeepCronUntilHealthy());
        assertTrue(shutdownSafety.isBootOnInstanceLeft());
    }

    @Test
    void shouldIgnoreFormerMainSwitch() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("enabled", true));

        assertFalse(shutdownSafety.isAbortOnShutdown());
        assertFalse(shutdownSafety.isKeepCronUntilHealthy());
        assertFalse(shutdownSafety.isBootOnInstanceLeft());
    }

    @Test
    void shouldFollowConfigurationChangesWithoutRestart() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        MockOsgi.modified(shutdownSafety, bundleContext, Collections.singletonMap("bootOnInstanceLeft", true));

        assertTrue(shutdownSafety.isBootOnInstanceLeft());
    }

    private ShutdownSafety activate(Map<String, Object> props) {
        ShutdownSafety shutdownSafety = new ShutdownSafety();
        MockOsgi.activate(shutdownSafety, bundleContext, props);
        return shutdownSafety;
    }
}
