package com.vml.es.aem.acm.core.code;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = CodeRepository.class)
@Designate(ocd = CodeRepository.Config.class)
public class CodeRepository {

    @ObjectClassDefinition(name = "AEM Content Manager - Code repository")
    public @interface Config {
        @AttributeDefinition(
                name = "Class Links",
                description =
                        "Mapping of class prefixes to their corresponding URLs in the format 'prefix=url'.")
        String[] classLinks() default {
            "com.vml.es.aem.acm.core=https://github.com/wttech/acm/blob/main/core/src/main/java",
            "org.apache.sling.api=https://github.com/apache/sling-org-apache-sling-api/tree/master/src/main/java"
        };
    }

    private final Map<String, String> CLASS_LINKS = new HashMap<>();

    @Activate
    @Modified
    public void activate(Config config) {
        for (String classLink : config.classLinks()) {
            String[] parts = classLink.split("=");
            if (parts.length == 2) {
                CLASS_LINKS.put(parts[0].trim(), parts[1].trim());
            }
        }
    }

    public Optional<String> linkToClass(String className) {
        return CLASS_LINKS.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(className, entry.getKey()))
                .findFirst()
                .map(entry -> String.format("%s/%s.java", entry.getValue(), StringUtils.replace(className, ".", "/")));
    }
}
