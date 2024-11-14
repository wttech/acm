package com.wttech.aem.migrator.core.script;

import org.apache.sling.api.resource.ResourceResolver;

import java.io.OutputStream;

public class ExecutionOptions {

    private ResourceResolver resourceResolver;

    private OutputStream outputStream = null;

    private ExecutionMode mode = ExecutionMode.EVALUATE;

    public ExecutionOptions(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
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
