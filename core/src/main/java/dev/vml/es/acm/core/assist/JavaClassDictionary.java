package dev.vml.es.acm.core.assist;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoResource;
import dev.vml.es.acm.core.util.StreamUtils;
import dev.vml.es.acm.core.util.YamlUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class JavaClassDictionary implements Serializable {

    public static final String ROOT = "/conf/acm/settings/assist/java";

    public static final String FILE_EXTENSION = "txt";

    private final String version;

    private final Map<String, List<String>> modules;

    public JavaClassDictionary(String version, Map<String, List<String>> modules) {
        this.version = version;
        this.modules = modules;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, List<String>> getModules() {
        return modules;
    }

    public static JavaClassDictionary determine(ResourceResolver resolver) {
        return byJavaVersion(resolver, javaVersion()).orElseGet(() -> highestJavaVersion(resolver)
                .orElseThrow(() -> new IllegalStateException("Java class dictionary cannot be determined!")));
    }

    public static Optional<JavaClassDictionary> byJavaVersion(ResourceResolver resolver, String javaVersion) {
        return Optional.ofNullable(javaVersion)
                .map(JavaClassDictionary::path)
                .map(resolver::getResource)
                .filter(Objects::nonNull)
                .filter(r -> r.isResourceType(JcrConstants.NT_FILE))
                .map(JavaClassDictionary::read);
    }

    public static Optional<JavaClassDictionary> highestJavaVersion(ResourceResolver resolver) {
        return Optional.ofNullable(resolver.getResource(ROOT))
                .map(r -> StreamUtils.asStream(r.listChildren()))
                .orElse(Stream.empty())
                .filter(r -> r.isResourceType(JcrConstants.NT_FILE))
                .sorted(Comparator.comparing(Resource::getName).reversed())
                .map(JavaClassDictionary::read)
                .findFirst();
    }

    public static JavaClassDictionary read(Resource resource) {
        try (InputStream input = RepoResource.of(resource).readFileAsStream()) {
            return YamlUtils.read(input, JavaClassDictionary.class);
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Cannot read Java class dictionary at path '%s'!", resource.getPath()), e);
        }
    }

    public static void save(ResourceResolver resolver, Map<String, List<String>> modules) {
        JavaClassDictionary dictionary = new JavaClassDictionary(javaVersion(), modules);
        RepoResource.of(resolver, dictionary.version).saveFile("application/x-yaml", output -> {
            try {
                YamlUtils.write(output, dictionary);
            } catch (IOException e) {
                throw new AcmException(
                        String.format("Cannot save Java class dictionary for version '%s'!", dictionary.version), e);
            }
        });
    }

    public static String path(String version) {
        return String.format("%s/%s.%s", ROOT, version, FILE_EXTENSION);
    }

    public static String path() {
        return path(System.getProperty("java.specification.version"));
    }

    public static String javaVersion() {
        return System.getProperty("java.specification.version");
    }
}
