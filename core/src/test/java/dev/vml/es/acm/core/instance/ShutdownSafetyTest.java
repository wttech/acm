package dev.vml.es.acm.core.instance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

class ShutdownSafetyTest {

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    @Test
    void shouldKeepAbortOnShutdownOffByDefault() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        assertFalse(shutdownSafety.isAbortOnShutdown());
    }

    @Test
    void shouldAbortOnShutdownWhenEnabled() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("enabled", true));

        assertTrue(shutdownSafety.isAbortOnShutdown());
    }

    @Test
    void shouldNotAbortOnShutdownWhenItsPartIsSwitchedOff() {
        Map<String, Object> props = new HashMap<>();
        props.put("enabled", true);
        props.put("abortOnShutdown", false);

        ShutdownSafety shutdownSafety = activate(props);

        assertFalse(shutdownSafety.isAbortOnShutdown());
    }

    @Test
    void shouldFollowConfigurationChangesWithoutRestart() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        MockOsgi.modified(shutdownSafety, bundleContext, Collections.singletonMap("enabled", true));

        assertTrue(shutdownSafety.isAbortOnShutdown());
    }

    private ShutdownSafety activate(Map<String, Object> props) {
        ShutdownSafety shutdownSafety = new ShutdownSafety();
        MockOsgi.activate(shutdownSafety, bundleContext, props);
        return shutdownSafety;
    }
}
