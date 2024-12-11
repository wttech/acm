package com.wttech.aem.contentor.core.code;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;

import java.io.OutputStream;

public class ExecutionContext {

    private final Executable executable;

    private final BundleContext bundleContext;

    private final ResourceResolver resourceResolver;

    private final History history;

    private OutputStream outputStream = null;

    private ExecutionMode mode = ExecutionMode.EVALUATE;

    public ExecutionContext(Executable executable, BundleContext bundleContext, ResourceResolver resourceResolver, History history) {
        this.executable = executable;
        this.bundleContext = bundleContext;
        this.resourceResolver = resourceResolver;
        this.history = history;
    }

    public Executable getExecutable() {
        return executable;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public History getHistory() {
        return history;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }
}
