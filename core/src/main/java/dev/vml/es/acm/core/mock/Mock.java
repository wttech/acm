package dev.vml.es.acm.core.mock;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoResource;
import org.apache.sling.api.resource.Resource;

public class Mock {

    private final Resource resource;

    public Mock(Resource resource) {
        this.resource = resource;
    }

    public String getId() {
        return getPath();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getContent() throws AcmException {
        return RepoResource.of(resource).readFileAsString();
    }
}
