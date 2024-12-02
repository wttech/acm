package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceSpliterator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ScriptRepository {

    public static final String ROOT = "/conf/contentor/settings/script";

    private final ResourceResolver resourceResolver;

    public ScriptRepository(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Optional<Script> read(String path) {
        return Optional.ofNullable(resourceResolver.getResource(path)).map(Script::new);
    }

    public Stream<Script> findAll() throws ContentorException {
        return ResourceSpliterator.stream(readRoot())
                .map(r -> Script.from(r).orElse(null))
                .filter(Objects::nonNull);
    }

    private Resource readRoot() throws ContentorException {
        Resource root = resourceResolver.getResource(ROOT);
        if (root == null) {
            throw new ContentorException(String.format("Scripts root path '%s' does not exist!", ROOT));
        }
        return root;
    }
}
