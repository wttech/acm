package dev.vml.es.acm.core.code;

import org.apache.sling.api.resource.ResourceResolver;

public class Locker {

    private final ResourceResolver resolver;

    public Locker(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isLocked(String name) {
        return false; // TODO
    }

    public void lock(String name) {
        // TODO ...
    }

    public void unlock(String name) {

    }
}
