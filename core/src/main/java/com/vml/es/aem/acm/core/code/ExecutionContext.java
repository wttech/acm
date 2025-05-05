package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.acl.Acl;
import com.vml.es.aem.acm.core.format.Formatter;
import com.vml.es.aem.acm.core.osgi.OsgiContext;
import com.vml.es.aem.acm.core.replication.Activator;
import com.vml.es.aem.acm.core.repo.Repo;
import groovy.lang.Binding;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionContext implements AutoCloseable {

    private final String id;

    private final ExecutionMode mode;

    private final Output output;

    private final Executor executor;

    private final Executable executable;

    private final OsgiContext osgiContext;

    private final ResourceResolver resourceResolver;

    private final Extender extender;

    private boolean history = true;

    private boolean debug = false;

    private final Arguments arguments = new Arguments();

    private Binding binding = new Binding();

    public ExecutionContext(
            String id,
            ExecutionMode mode,
            Executor executor,
            Executable executable,
            OsgiContext osgiContext,
            ResourceResolver resourceResolver) {
        this.id = id;
        this.mode = mode;
        this.output = mode == ExecutionMode.RUN ? new OutputFile(id) : new OutputString();
        this.executor = executor;
        this.executable = executable;
        this.osgiContext = osgiContext;
        this.resourceResolver = resourceResolver;
        this.binding = createBinding(resourceResolver);
        this.extender = new Extender(this);
    }

    public String getId() {
        return id;
    }

    public Output getOutput() {
        return output;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Executable getExecutable() {
        return executable;
    }

    public Extender getExtender() {
        return extender;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public OsgiContext getOsgiContext() {
        return osgiContext;
    }

    public ExecutionMode getMode() {
        return mode;
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

    public Arguments getArguments() {
        return arguments;
    }

    public Binding getBinding() {
        return binding;
    }

    @SuppressWarnings("unchecked")
    public List<Variable> getBindingVariables() {
        Map<String, Object> variables = binding.getVariables();
        return variables.entrySet().stream()
                .map(entry -> new Variable(entry.getKey(), entry.getValue().getClass()))
                .collect(Collectors.toList());
    }

    private Binding createBinding(ResourceResolver resourceResolver) {
        Binding result = new Binding();

        result.setVariable("args", arguments);
        result.setVariable("condition", new Condition(this));
        result.setVariable("log", createLogger(executable));
        result.setVariable("out", new CodePrintStream(this));
        result.setVariable("resourceResolver", resourceResolver);
        result.setVariable("osgi", osgiContext);
        result.setVariable("repo", new Repo(resourceResolver));
        result.setVariable("acl", new Acl(resourceResolver));
        result.setVariable("formatter", new Formatter());
        result.setVariable("activator", new Activator(resourceResolver, osgiContext.getReplicator()));

        return result;
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), executable.getId()));
    }

    @Override
    public void close() {
        output.close();
    }

    public void variable(String name, Object value) {
        binding.setVariable(name, value);
    }
}
