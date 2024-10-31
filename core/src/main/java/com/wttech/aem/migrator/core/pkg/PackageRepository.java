package com.wttech.aem.migrator.core.pkg;

import java.util.Optional;
import org.apache.sling.api.resource.ResourceResolver;

public class PackageRepository {

    private final ResourceResolver resourceResolver;

    public PackageRepository(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Optional<Package> read(String pid) {
        return Optional.empty();
    }
}
