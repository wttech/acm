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
    void shouldBeOffByDefault() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        assertFalse(shutdownSafety.isEnabled());
    }

    @Test
    void shouldBeOnWhenEnabled() {
        ShutdownSafety shutdownSafety = activate(Collections.singletonMap("enabled", true));

        assertTrue(shutdownSafety.isEnabled());
    }

    @Test
    void shouldFollowConfigurationChangesWithoutRestart() {
        ShutdownSafety shutdownSafety = activate(Collections.emptyMap());

        MockOsgi.modified(shutdownSafety, bundleContext, Collections.singletonMap("enabled", true));

        assertTrue(shutdownSafety.isEnabled());
    }

    private ShutdownSafety activate(Map<String, Object> props) {
        ShutdownSafety shutdownSafety = new ShutdownSafety();
        MockOsgi.activate(shutdownSafety, bundleContext, props);
        return shutdownSafety;
    }
}
