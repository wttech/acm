package com.wttech.aem.contentor.core.code;

import java.io.OutputStream;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;

public class ExecutionContext {

    private final Executable executable;

    private final BundleContext bundleContext;

    private final ResourceResolver resourceResolver;

    private OutputStream outputStream = null;

    private ExecutionMode mode = ExecutionMode.EVALUATE;

    private boolean history = true;

    private String id = ExecutionId.generate();

    public ExecutionContext(Executable executable, BundleContext bundleContext, ResourceResolver resourceResolver) {
        this.executable = executable;
        this.bundleContext = bundleContext;
        this.resourceResolver = resourceResolver;
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

    public OutputStream getOutputStream() {
        return outputStream;
    }

    protected void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }
}
