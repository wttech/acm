package com.wttech.aem.contentor.core.osgi;

import java.util.Optional;

public class OsgiFacade {

    private final OsgiContext context;

    public OsgiFacade(OsgiContext context) {
        this.context = context;
    }

    public <T> T requireService(Class<T> clazz) {
        return getService(clazz).orElseThrow(() -> new IllegalStateException("Service not found: " + clazz.getName()));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(Class<T> clazz) {
        return Optional.ofNullable((T) context.getBundleContext().getServiceReference(clazz));
    }
}
