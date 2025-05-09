package com.vml.es.aem.acm.core.mock;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Arrays;
import java.util.List;

@Component(service = MockManager.class, immediate = true)
public class MockManager {

    public static final String INTERNAL_DIR = "internal";

    public static final String FAIL_PATH = INTERNAL_DIR + "/fail.groovy";

    public static final String MISSING_PATH = INTERNAL_DIR + "/missing.groovy";

    public static final List<String> SPECIAL_PATHS = Arrays.asList(FAIL_PATH, MISSING_PATH);

    private Config config;

    @ObjectClassDefinition(name = "AEM Content Manager - Mock Manager")
    public @interface Config {

        @AttributeDefinition(name = "Search paths", description = "JCR repository paths to search for mock resources.")
        String[] searchPaths() default {"/conf/stubs"};

        @AttributeDefinition(
                name = "Classifier",
                description = "Resource name part used to distinguish stubs from other files.")
        String classifier() default "stub";
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public List<String> getSearchPaths() {
        return Arrays.asList(config.searchPaths());
    }

    public String getClassifier() {
        return config.classifier();
    }

    public boolean isSpecial(Mock mock) {
        return SPECIAL_PATHS.stream().anyMatch(n -> StringUtils.endsWith(mock.getId(), "/" + n));
    }
}
