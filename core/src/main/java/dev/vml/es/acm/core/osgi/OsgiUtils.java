package dev.vml.es.acm.core.osgi;

import org.osgi.framework.Bundle;

public final class OsgiUtils {

    private OsgiUtils() {
        // Utility class
    }

    public static String getStateName(int state) {
        switch (state) {
            case Bundle.UNINSTALLED:
                return "uninstalled";
            case Bundle.INSTALLED:
                return "installed";
            case Bundle.RESOLVED:
                return "resolved";
            case Bundle.STARTING:
                return "starting";
            case Bundle.STOPPING:
                return "stopping";
            case Bundle.ACTIVE:
                return "active";
            default:
                return String.format("state '%d'", state);
        }
    }
}
