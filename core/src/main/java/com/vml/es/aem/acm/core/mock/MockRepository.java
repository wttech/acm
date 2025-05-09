package com.vml.es.aem.acm.core.mock;

import java.util.Optional;
import java.util.stream.Stream;

import com.vml.es.aem.acm.core.util.ResourceSpliterator;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;

public class MockRepository {

    private final MockManager manager;

    private final ResourceResolver resolver;

    public MockRepository(MockManager manager, ResourceResolver resolver) {
        this.manager = manager;
        this.resolver = resolver;
    }

    public Stream<Resource> findStubs() throws MockException {
        Stream<Resource> result = Stream.empty();
        for (String path : manager.getSearchPaths()) {
            Resource root = resolver.getResource(path);
            if (root == null) {
                throw new MockException(String.format("Cannot read mock search path '%s'!", path));
            }
            Stream<Resource> stream = ResourceSpliterator.stream(root, this::isStub);
            result = Stream.concat(result, stream);
        }
        return result;
    }

    private boolean isStub(Resource resource) {
        return resource.isResourceType(JcrConstants.NT_FILE) && resource.getName().endsWith(manager.getClassifier());
    }

    public Optional<Resource> findResource(String subPath) {
        for (String path : manager.getSearchPaths()) {
            Resource result = resolver.getResource(String.format("%s/%s", path, subPath));
            if (result != null) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    public Optional<Resource> findStub(String subPath) {
        return findResource(subPath).filter(this::isStub);
    }

    public Optional<Resource> findSpecialStub(String subPath) {
        return findResource(subPath);
    }
}