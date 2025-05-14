package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.acl.Acl;
import com.vml.es.aem.acm.core.code.script.ExtensionScript;
import com.vml.es.aem.acm.core.format.Formatter;
import com.vml.es.aem.acm.core.mock.MockContext;
import com.vml.es.aem.acm.core.osgi.OsgiContext;
import com.vml.es.aem.acm.core.replication.Activator;
import com.vml.es.aem.acm.core.repo.Repo;
import com.vml.es.aem.acm.core.script.ScriptRepository;
import com.vml.es.aem.acm.core.script.ScriptType;
import groovy.lang.Binding;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.LoggerFactory;

public class CodeContext {

    private final OsgiContext osgiContext;

    private final ResourceResolver resourceResolver;

    private final Binding binding;

    private final List<ExtensionScript> scripts;

    public CodeContext(OsgiContext osgiContext, ResourceResolver resourceResolver) {
        this.osgiContext = osgiContext;
        this.resourceResolver = resourceResolver;
        this.binding = createBinding(osgiContext, resourceResolver);
        this.scripts = new ScriptRepository(resourceResolver)
                .findAll(ScriptType.EXTENSION)
                .map(s -> new ExtensionScript(this, s))
                .collect(Collectors.toList());
    }

    public void prepareRun(ExecutionContext executionContext) {
        for (ExtensionScript script : scripts) {
            script.prepareRun(executionContext);
        }
    }

    public void completeRun(Execution execution) {
        for (ExtensionScript script : scripts) {
            script.completeRun(execution);
        }
    }

    public void prepareMock(MockContext mockContext) {
        for (ExtensionScript script : scripts) {
            script.prepareMock(mockContext);
        }
    }

    public Binding createBinding(OsgiContext osgiContext, ResourceResolver resourceResolver) {
        Binding result = new Binding();

        result.setVariable("log", LoggerFactory.getLogger(getClass()));
        result.setVariable("resourceResolver", resourceResolver);
        result.setVariable("osgi", osgiContext);
        result.setVariable("repo", new Repo(resourceResolver));
        result.setVariable("acl", new Acl(resourceResolver));
        result.setVariable("formatter", new Formatter());
        result.setVariable("activator", new Activator(resourceResolver, osgiContext.getReplicator()));

        return result;
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

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public OsgiContext getOsgiContext() {
        return osgiContext;
    }
}
