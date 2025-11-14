package dev.vml.es.acm.core.state;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.ResourceResolver;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.repo.Repo;

public class Permissions implements Serializable {

    public enum Feature {
        CONSOLE_VIEW,
        CONSOLE_EXECUTE,
        DASHBOARD_VIEW,
        EXECUTION_LIST,
        EXECUTION_VIEW,
        MAINTENANCE_VIEW,
        MAINTENANCE_MANAGE,
        SCRIPT_LIST,
        SCRIPT_VIEW,
        SCRIPT_EXECUTE,
        SCRIPT_MANAGE,
        SNIPPET_LIST,
    }

    private static final String FEATURE_ROOT = AcmConstants.APPS_ROOT + "/feature"; 

    private final Map<String, Boolean> features;

    public Permissions(ResourceResolver resolver) {
        this.features = authorizeFeatures(resolver);
    }

    public static boolean check(Feature feature, ResourceResolver resolver) {
        return authorizeFeature(feature, resolver);
    }

    public static boolean authorizeFeature(Feature f, ResourceResolver resolver) {
        return Repo.quiet(resolver).get(FEATURE_ROOT + "/" + featureNodePath(f)).exists();
    }

    public Map<String, Boolean> authorizeFeatures(ResourceResolver resolver) {
        return Arrays.stream(Feature.values())
            .collect(Collectors.toMap(Permissions::featureId, f -> authorizeFeature(f, resolver), (a, b) -> b, LinkedHashMap::new));
    }

    private static String featureId(Feature f) {
        return f.name().toLowerCase().replace("_", ".");
    }

    private static String featureNodePath(Feature f) {
        return f.name().toLowerCase().replace("_", "/");
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }
}
