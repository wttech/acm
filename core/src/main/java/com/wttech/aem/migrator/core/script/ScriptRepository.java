package com.wttech.aem.migrator.core.script;

import java.util.Optional;
import org.apache.sling.api.resource.ResourceResolver;

public class ScriptRepository {

    private final ResourceResolver resourceResolver;

    public ScriptRepository(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Optional<Script> read(String path) {
        return Optional.ofNullable(resourceResolver.getResource(path)).map(Script::new);
    }
}
