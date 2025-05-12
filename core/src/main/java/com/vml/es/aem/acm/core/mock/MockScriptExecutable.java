package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.code.ArgumentValues;
import com.vml.es.aem.acm.core.code.Executable;
import com.vml.es.aem.acm.core.repo.RepoResource;
import org.apache.sling.api.resource.Resource;

public class MockScriptExecutable implements Executable {

    private final Resource resource;

    public MockScriptExecutable(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getId() {
        return resource.getPath();
    }

    @Override
    public String getContent() throws AcmException {
        return RepoResource.of(resource).readFileAsString();
    }

    @Override
    public ArgumentValues getArguments() {
        return new ArgumentValues();
    }
}
