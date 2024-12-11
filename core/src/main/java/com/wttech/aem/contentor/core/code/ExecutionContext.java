package com.wttech.aem.contentor.core.code;

import org.apache.sling.api.resource.ResourceResolver;

import java.io.OutputStream;

public class ExecutionContext {

    private final Executable executable;

    private final ResourceResolver resourceResolver;

    private final History history;

    private OutputStream outputStream = null;

    private ExecutionMode mode = ExecutionMode.EVALUATE;

    public ExecutionContext(Executable executable, ResourceResolver resourceResolver, History history) {
        this.executable = executable;
        this.resourceResolver = resourceResolver;
        this.history = history;
    }

    public Executable getExecutable() {
        return executable;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
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
