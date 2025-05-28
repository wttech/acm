package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.assist.JavaDictionary;
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
                                + " Used to generate links to class documentation in code assistance.")
        String[] classLinkMappings() default {
            "dev.vml.es.acm.core=https://github.com/wttech/acm/blob/main/core/src/main/java/%s.java",
            "org.apache.commons.lang3=https://commons.apache.org/proper/commons-lang/apidocs/%s.html",
            "org.apache.sling.api=https://github.com/apache/sling-org-apache-sling-api/tree/master/src/main/java/%s.java",
            "org.apache.sling.models=https://github.com/apache/sling-org-apache-sling-models-api/tree/master/src/main/java/%s.java",
            "com.day.cq=https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/%s.html",
            "com.adobe.cq=https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/%s.html",
            "com.adobe.granite=https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/%s.html",
        };

        @AttributeDefinition(
                name = "Class Link for RT Jar",
                description = "URL to the Java 8 documentation, used for linking classes from runtime jar.")
        String classLinkRtJar() default "https://docs.oracle.com/javase/8/docs/api/%s.html";

        @AttributeDefinition(
                name = "Class Link for JMS",
                description = "URL to the Java 9+ documentation, used for linking classes from JMS.")
        String classLinkJms() default "https://docs.oracle.com/en/java/javase/%s/docs/api/%s/%s.html";
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
        if (classInfo.getModule() == null) {
            return linkToClass(classInfo.getClassName());
        } else if (JavaDictionary.RTJAR_MODULE.equals(classInfo.getModule())) {
            return Optional.of(
                    String.format(config.classLinkRtJar(), StringUtils.replace(classInfo.getClassName(), ".", "/")));
        } else {
            return Optional.of(String.format(
                    config.classLinkJms(),
                    JavaDictionary.currentVersion(),
                    classInfo.getModule(),
                    StringUtils.replace(classInfo.getClassName(), ".", "/")));
        }
    }

    public Optional<String> linkToClass(String className) {
        return classLinks.entrySet().stream()
                .filter(entry -> StringUtils.startsWith(className, entry.getKey()))
                .findFirst()
                .map(e -> String.format(e.getValue(), StringUtils.replace(className, ".", "/")));
    }
}
