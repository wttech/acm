package com.wttech.aem.contentor.core.assist;

import org.apache.sling.api.resource.Resource;

public class ResourceSuggestion implements Suggestion {
    private final Resource resource;

    public ResourceSuggestion(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getKind() {
        return "resource";
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getInsertText() {
        return resource.getPath();
    }

    @Override
    public String getInfo() {
        return resource.getResourceType();
    }
}
