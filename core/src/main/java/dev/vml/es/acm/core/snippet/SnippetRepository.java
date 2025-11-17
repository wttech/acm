package dev.vml.es.acm.core.snippet;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoResource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class SnippetRepository {

    public static final String ROOT = AcmConstants.SETTINGS_ROOT + "/snippet";

    private final ResourceResolver resourceResolver;

    public SnippetRepository(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Optional<Snippet> read(String path) {
        return Optional.ofNullable(path)
                .filter(p -> StringUtils.startsWith(p, SnippetType.AVAILABLE.root() + "/"))
                .map(resourceResolver::getResource)
                .flatMap(Snippet::from);
    }

    public Stream<Snippet> readAll(List<String> paths) {
        return paths.stream()
                .filter(p -> StringUtils.startsWith(p, SnippetType.AVAILABLE.root() + "/"))
                .map(resourceResolver::getResource)
                .map(Snippet::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<Snippet> findAll() throws AcmException {
        return RepoResource.of(getRoot())
                .descendants()
                .map(RepoResource::resolve)
                .map(Snippet::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Resource getRoot() throws AcmException {
        Resource root = resourceResolver.getResource(ROOT);
        if (root == null) {
            throw new AcmException(String.format("Snippet root does not exist at path '%s'!", ROOT));
        }
        return root;
    }
}
