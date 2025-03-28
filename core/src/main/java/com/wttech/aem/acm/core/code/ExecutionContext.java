package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.util.NullOutputStream;
import java.io.OutputStream;
import org.apache.sling.api.resource.ResourceResolver;

public class ExecutionContext {

    private final Executor executor;

    private final Executable executable;

    private final OsgiContext osgiContext;

    private final ResourceResolver resourceResolver;

    private OutputStream outputStream = new NullOutputStream();

    private ExecutionMode mode = ExecutionMode.RUN;

    private boolean history = true;

    private boolean debug = false;

    private String id = ExecutionId.generate();

    public ExecutionContext(
            Executor executor, Executable executable, OsgiContext osgiContext, ResourceResolver resourceResolver) {
        this.executor = executor;
        this.executable = executable;
        this.osgiContext = osgiContext;
        this.resourceResolver = resourceResolver;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Executable getExecutable() {
        return executable;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public OsgiContext getOsgiContext() {
        return osgiContext;
    }

    public ExecutionFileOutput getFileOutput() {
        return new ExecutionFileOutput(id);
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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
