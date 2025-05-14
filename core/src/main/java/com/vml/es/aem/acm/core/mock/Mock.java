package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.repo.RepoResource;
import org.apache.sling.api.resource.Resource;

public class Mock {

    private final Resource resource;

    public Mock(Resource resource) {
        this.resource = resource;
    }

    public String getId() {
        return resource.getPath();
    }

    public String getContent() throws AcmException {
        return RepoResource.of(resource).readFileAsString();
    }
}
