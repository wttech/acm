package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.osgi.ClassInfo;
import java.util.Collections;
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

    @ObjectClassDefinition(name = "AEM Content Manager - Code Repository")
    public @interface Config {
        @AttributeDefinition(
                name = "Class Links",
                description =
                        "Mapping of class prefixes to their corresponding documentation URLs in the format 'package=url'. Used to generate links to class documentation in code assistance.")
        String[] classLinks() default {
            "dev.vml.es.acm.core=https://github.com/wttech/acm/blob/main/core/src/main/java",
            "org.apache.sling.api=https://github.com/apache/sling-org-apache-sling-api/tree/master/src/main/java"
        };
    }

    private Map<String, String> classLinks = Collections.emptyMap();

    @Activate
    @Modified
    public void activate(Config config) {
        this.classLinks = parseClassLinks(config.classLinks());
    }

    private Map<String, String> parseClassLinks(String[] classLinks) {
        Map<String, String> result = new HashMap<>();
        for (String classLink : classLinks) {
            String[] parts = classLink.split("=");
            if (parts.length == 2) {
                result.put(parts[0].trim(), parts[1].trim());
            }
        }
        return result;
    }

    // TODO consider 'module' and 'JavaClassDictionary.version()' to generate links to specific versions
    public Optional<String> linkToClass(ClassInfo classInfo) {
        return classLinks.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(classInfo.getClassName(), entry.getKey()))
                .findFirst()
                .map(entry -> String.format(
                        "%s/%s.java", entry.getValue(), StringUtils.replace(classInfo.getClassName(), ".", "/")));
    }

    // TODO unify it with method above if possible
    public Optional<String> linkToClass(String className) {
        return classLinks.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(className, entry.getKey()))
                .findFirst()
                .map(entry -> String.format("%s/%s.java", entry.getValue(), StringUtils.replace(className, ".", "/")));
    }
}
