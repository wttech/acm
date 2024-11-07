package com.wttech.aem.migrator.core.script;

import java.io.OutputStream;
import org.apache.sling.api.resource.ResourceResolver;

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
