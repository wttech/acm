package com.wttech.aem.acm.core.code;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class CodeRepository {

    // TODO make it configurable
    private static final Map<String, String> CLASS_LINKS = new HashMap<>();

    static {
        CLASS_LINKS.put(
                "com.wttech.aem.acm.core",
                "https://github.com/wttech/acm/blob/main/core/src/main/java");
        CLASS_LINKS.put(
                "org.apache.sling.api",
                "https://github.com/apache/sling-org-apache-sling-api/tree/master/src/main/java");
    }

    private CodeRepository() {
        // intentionally empty
    }

    public static Optional<String> linkToClass(String className) {
        return CLASS_LINKS.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(className, entry.getKey()))
                .findFirst()
                .map(entry -> String.format("%s/%s.java", entry.getValue(), StringUtils.replace(className, ".", "/")));
    }
}
