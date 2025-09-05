package dev.vml.es.acm.core.osgi;

import java.util.Map;
import org.osgi.framework.Bundle;

public final class OsgiUtils {

    private static final Map<Integer, String> BUNDLE_STATE_NAMES = Map.of(
            Bundle.UNINSTALLED, "uninstalled",
            Bundle.INSTALLED, "installed",
            Bundle.RESOLVED, "resolved",
            Bundle.STARTING, "starting",
            Bundle.STOPPING, "stopping",
            Bundle.ACTIVE, "active");

    private OsgiUtils() {
        // intentionally empty
    }

    public static String bundleStateName(int state) {
        return BUNDLE_STATE_NAMES.getOrDefault(state, String.format("state '%d'", state));
    }
}
