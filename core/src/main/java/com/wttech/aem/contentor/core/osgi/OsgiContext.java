package com.wttech.aem.contentor.core.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = OsgiContext.class)
public class OsgiContext {

    @Reference
    private InstanceInfo instanceInfo;

    private BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }
}
