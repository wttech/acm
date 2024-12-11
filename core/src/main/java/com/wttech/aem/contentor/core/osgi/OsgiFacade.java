package com.wttech.aem.contentor.core.osgi;

import org.osgi.framework.BundleContext;

import java.util.Optional;

public class OsgiFacade {

    private final BundleContext bundleContext;

    public OsgiFacade(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    public <T> T requireService(Class<T> clazz) {
        return getService(clazz).orElseThrow(() -> new IllegalStateException("Service not found: " + clazz.getName()));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(Class<T> clazz) {
        return Optional.ofNullable((T) bundleContext.getServiceReference(clazz));
    }
}
