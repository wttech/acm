package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.repo.RepoResource;
import org.apache.commons.lang3.StringUtils;
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

    public String getDirPath() {
        return StringUtils.substringBeforeLast(getId(), "/");
    }

    public String resolvePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        return getDirPath() + "/" + path;
    }
}
