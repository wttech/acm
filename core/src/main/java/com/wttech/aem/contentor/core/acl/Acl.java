package com.wttech.aem.contentor.core.acl;

import org.apache.sling.api.resource.ResourceResolver;

public class Acl {

    private final ResourceResolver resourceResolver;

    public Acl(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public void createUser() {
        // TODO implement
    }

    public void createGroup() {
        // TODO implement
    }

    public void purge(String path) {
        // TODO implement
    }
}
