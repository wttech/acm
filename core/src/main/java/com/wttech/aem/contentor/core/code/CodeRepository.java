package com.wttech.aem.contentor.core.code;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class CodeRepository {

    public static final String CLASS_LINK_BASE_URL =
            "https://github.com/wunderman-thompson/wtpl-aem-contentor/blob/main/core/src/main/java";

    public static final String CLASS_LINK_JAVA_PACKAGE = "com.wttech.aem.contentor";

    private CodeRepository() {
        // intentionally empty
    }

    public static Optional<String> linkToClass(String className) {
        if (!StringUtils.startsWith(className, CLASS_LINK_JAVA_PACKAGE + ".")) {
            return Optional.empty();
        }
        return Optional.of(String.format("%s/%s.java", CLASS_LINK_BASE_URL, StringUtils.replace(className, ".", "/")));
    }
}
