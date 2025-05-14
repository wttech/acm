package com.vml.es.aem.acm.core.assist;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.repo.RepoResource;
import com.vml.es.aem.acm.core.util.StreamUtils;
import com.vml.es.aem.acm.core.util.YamlUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class JavaClassDictionary {

    public static final String ROOT = "/conf/acm/settings/assist/java";

    public static final String FILE_EXTENSION = "txt";

    public static final String JAVA_VERSION = System.getProperty("java.specification.version");

    private final Resource resource;

    public JavaClassDictionary(Resource resource) {
        this.resource = resource;
    }

    public static JavaClassDictionary determine(ResourceResolver resolver) {
        return byJavaVersion(resolver, JAVA_VERSION).orElseGet(() -> highestJavaVersion(resolver)
                .orElseThrow(() -> new IllegalStateException("Java class dictionary cannot be determined!")));
    }

    public static Optional<JavaClassDictionary> byJavaVersion(ResourceResolver resolver, String javaVersion) {
        return Optional.ofNullable(javaVersion)
                .map(JavaClassDictionary::path)
                .map(resolver::getResource)
                .filter(Objects::nonNull)
                .filter(r -> r.isResourceType(JcrConstants.NT_FILE))
                .map(JavaClassDictionary::new);
    }

    public static Optional<JavaClassDictionary> highestJavaVersion(ResourceResolver resolver) {
        return Optional.ofNullable(resolver.getResource(ROOT))
                .map(r -> StreamUtils.asStream(r.listChildren()))
                .orElse(Stream.empty())
                .filter(r -> r.isResourceType(JcrConstants.NT_FILE))
                .sorted(Comparator.comparing(Resource::getName).reversed())
                .map(JavaClassDictionary::new)
                .findFirst();
    }

    public static String path(String version) {
        return String.format("%s/%s.%s", ROOT, version, FILE_EXTENSION);
    }

    public static String path() {
        return path(System.getProperty("java.specification.version"));
    }

    public static String buildDocsUrl(String className, String module) {
        if (!className.startsWith("java")) {
            return null;
        }
        if ("1.8".equals(JAVA_VERSION)) {
            return String.format("https://docs.oracle.com/javase/8/docs/api/%s.html", className.replace(".", "/"));
        }
        return String.format(
                "https://docs.oracle.com/en/java/javase/%s/%s/docs/api/%s.html",
                JAVA_VERSION, module, className.replace(".", "/"));
    }

    public JavaDictionary getClasses() {
        try (InputStream input = RepoResource.of(resource).readFileAsStream()) {
            return YamlUtils.readYaml(input, JavaDictionary.class);
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Cannot read Java class dictionary at path '%s'!", resource.getPath()), e);
        }
    }
}
