package dev.vml.es.acm.core.assist;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoResource;
import dev.vml.es.acm.core.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class JavaClassDictionary {

    public static final String ROOT = "/conf/acm/settings/assist/java";

    public static final String FILE_EXTENSION = "txt";

    private final Resource resource;

    public JavaClassDictionary(Resource resource) {
        this.resource = resource;
    }

    public static JavaClassDictionary determine(ResourceResolver resolver) {
        return byJavaVersion(resolver, System.getProperty("java.specification.version"))
                .orElseGet(() -> highestJavaVersion(resolver)
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

    public Stream<String> getClasses() {
        try (InputStream input = RepoResource.of(resource).readFileAsStream()) {
            return StreamUtils.asStream(IOUtils.lineIterator(input, StandardCharsets.UTF_8))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank);
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Cannot read Java class dictionary at path '%s'!", resource.getPath()), e);
        }
    }
}
