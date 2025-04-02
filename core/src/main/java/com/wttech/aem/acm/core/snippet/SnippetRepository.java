package com.wttech.aem.acm.core.snippet;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.util.ResourceSpliterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public class SnippetRepository {

    public static final String ROOT = "/conf/acm/settings/snippet";

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
        return ResourceSpliterator.stream(getOrCreateRoot())
                .map(Snippet::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Resource getOrCreateRoot() throws AcmException {
        try {
            return ResourceUtil.getOrCreateResource(
                    resourceResolver,
                    ROOT,
                    JcrResourceConstants.NT_SLING_FOLDER,
                    JcrResourceConstants.NT_SLING_FOLDER,
                    true);
        } catch (Exception e) {
            throw new AcmException(String.format("Failed to get or create snippet root '%s'", ROOT), e);
        }
    }
}
