package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.assist.JavaClassDictionary;
import dev.vml.es.acm.core.format.TemplateFormatter;
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
                name = "Class Link Mappings",
                description =
                        "Mapping of class prefixes to their corresponding documentation URLs in the format 'package=url'."
                                + " Used to generate links to class documentation in code assistance."
                                + " Available variables: ${pkg} - package name, ${clazz} - class name.")
        String[] classLinkMappings() default {
            "dev.vml.es.acm.core=https://github.com/wttech/acm/blob/main/core/src/main/java/${pkg}/${clazz}.java",
            "org.apache.sling.api=https://github.com/apache/sling-org-apache-sling-api/tree/master/src/main/java/${pkg}/${clazz}.java",
        };

        @AttributeDefinition(
                name = "Class Link for RT Jar",
                description = "URL to the Java 8 documentation, used for linking classes from runtime jar."
                        + " Available variables: ${clazz} - class name.")
        String classLinkRtJar() default "https://docs.oracle.com/javase/8/docs/api/${clazz}.html";

        @AttributeDefinition(
                name = "Class Link for JMS",
                description = "URL to the Java 9+ documentation, used for linking classes from JMS."
                        + " Available variables: " + "${module} - module name, ${clazz} - class name.")
        String classLinkJms() default "https://docs.oracle.com/en/java/javase/${module}/docs/api/${clazz}.html";
    }

    private Config config;

    private Map<String, String> classLinks = Collections.emptyMap();

    @Activate
    @Modified
    public void activate(Config config) {
        this.config = config;
        this.classLinks = parseClassLinks(config.classLinkMappings());
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

    public Optional<String> linkToClass(ClassInfo classInfo) {
        TemplateFormatter templateFormatter = new TemplateFormatter();
        if (classInfo.getModule() == null) {
            return linkToClass(classInfo.getClassName());
        } else if (JavaClassDictionary.RTJAR_MODULE.equals(classInfo.getModule())) {
            Map<String, String> params =
                    Collections.singletonMap("clazz", StringUtils.replace(classInfo.getClassName(), ".", "/"));
            return Optional.of(templateFormatter.renderString(config.classLinkRtJar(), params));
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("module", classInfo.getModule());
            params.put("clazz", StringUtils.replace(classInfo.getClassName(), ".", "/"));
            return Optional.of(templateFormatter.renderString(config.classLinkJms(), params));
        }
    }

    public Optional<String> linkToClass(String className) {
        TemplateFormatter templateFormatter = new TemplateFormatter();
        return classLinks.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(className, entry.getKey()))
                .findFirst()
                .map(entry -> templateFormatter.renderString(
                        entry.getValue(), mappingLinkParams(entry.getValue(), className)));
    }

    private Map<String, String> mappingLinkParams(String pkg, String className) {
        Map<String, String> params = new HashMap<>();
        params.put("pkg", pkg);
        params.put("clazz", className);
        return params;
    }
}
