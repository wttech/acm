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

public class JavaDictionary implements Serializable {

    public static final String ROOT = "/conf/acm/settings/assist/java";

    public static final String RTJAR_MODULE = "rtjar";

    private String version;

    private Map<String, List<String>> modules;

    @SuppressWarnings("unused")
    public JavaDictionary() {
        // for deserialization
    }

    public JavaDictionary(String version, Map<String, List<String>> modules) {
        this.version = version;
        this.modules = modules;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, List<String>> getModules() {
        return modules;
    }

    public static JavaDictionary determine(ResourceResolver resolver) {
        return byVersion(resolver, currentVersion()).orElseGet(() -> forHighestVersion(resolver)
                .orElseThrow(() -> new IllegalStateException("Java dictionary cannot be determined!")));
    }

    public static Optional<JavaDictionary> byVersion(ResourceResolver resolver, String javaVersion) {
        return Optional.ofNullable(javaVersion)
                .map(JavaDictionary::path)
                .map(resolver::getResource)
                .filter(Objects::nonNull)
                .filter(r -> r.isResourceType(JcrConstants.NT_FILE))
                .map(JavaDictionary::read);
    }

    public static Optional<JavaDictionary> forHighestVersion(ResourceResolver resolver) {
        return Optional.ofNullable(resolver.getResource(ROOT))
                .map(r -> StreamUtils.asStream(r.listChildren()))
                .orElse(Stream.empty())
                .filter(r -> r.isResourceType(JcrConstants.NT_FILE))
                .sorted(Comparator.comparing(Resource::getName).reversed())
                .map(JavaDictionary::read)
                .findFirst();
    }

    public static JavaDictionary read(Resource resource) {
        try (InputStream input = RepoResource.of(resource).readFileAsStream()) {
            return YamlUtils.read(input, JavaDictionary.class);
        } catch (IOException e) {
            throw new AcmException(String.format("Cannot read Java dictionary at path '%s'!", resource.getPath()), e);
        }
    }

    public static void save(ResourceResolver resolver, Map<String, List<String>> modules) {
        JavaDictionary dictionary = new JavaDictionary(currentVersion(), modules);
        RepoResource.of(resolver, path()).saveFile(YamlUtils.MIME_TYPE, output -> {
            try {
                YamlUtils.write(output, dictionary);
            } catch (IOException e) {
                throw new AcmException(
                        String.format("Cannot save Java dictionary for version '%s'!", dictionary.version), e);
            }
        });
    }

    public static String path(String version) {
        return String.format("%s/%s.%s", ROOT, version, YamlUtils.EXTENSION);
    }

    public static String path() {
        return path(currentVersion());
    }

    public static String currentVersion() {
        return System.getProperty("java.specification.version");
    }
}
