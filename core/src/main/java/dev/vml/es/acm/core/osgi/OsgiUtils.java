package dev.vml.es.acm.core.osgi;

import org.osgi.framework.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for OSGi operations.
 */
public final class OsgiUtils {

    private static final Map<Integer, String> BUNDLE_STATE_NAMES;
    
    static {
        Map<Integer, String> stateNames = new HashMap<>();
        stateNames.put(Bundle.UNINSTALLED, "uninstalled");
        stateNames.put(Bundle.INSTALLED, "installed");
        stateNames.put(Bundle.RESOLVED, "resolved");
        stateNames.put(Bundle.STARTING, "starting");
        stateNames.put(Bundle.STOPPING, "stopping");
        stateNames.put(Bundle.ACTIVE, "active");
        BUNDLE_STATE_NAMES = stateNames;
    }

    private OsgiUtils() {
        // intentionally empty
    }

    public static String bundleStateName(int state) {
        return BUNDLE_STATE_NAMES.getOrDefault(state, String.format("state '%d'", state));
    }
}
