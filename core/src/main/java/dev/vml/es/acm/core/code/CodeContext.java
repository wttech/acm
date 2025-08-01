package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.acl.Acl;
import dev.vml.es.acm.core.code.script.ExtensionScript;
import dev.vml.es.acm.core.format.Formatter;
import dev.vml.es.acm.core.mock.MockContext;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.replication.Activator;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptType;
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

    private final List<ExtensionScript> extensionScripts;

    private final Locker locker;

    public CodeContext(OsgiContext osgiContext, ResourceResolver resourceResolver) {
        this.osgiContext = osgiContext;
        this.resourceResolver = resourceResolver;
        this.locker = new Locker(resourceResolver);
        this.binding = createBinding();
        this.extensionScripts = findExtensionScripts();
    }

    private List<ExtensionScript> findExtensionScripts() {
        return new ScriptRepository(resourceResolver)
                .findAll(ScriptType.EXTENSION)
                .map(s -> new ExtensionScript(this, s))
                .collect(Collectors.toList());
    }

    public Binding createBinding() {
        Binding result = new Binding();

        result.setVariable("log", LoggerFactory.getLogger(getClass()));
        result.setVariable("resourceResolver", resourceResolver);
        result.setVariable("locker", locker);
        result.setVariable("osgi", osgiContext);
        result.setVariable("repo", new Repo(resourceResolver));
        result.setVariable("acl", new Acl(resourceResolver));
        result.setVariable("formatter", new Formatter());
        result.setVariable("activator", new Activator(resourceResolver, osgiContext.getReplicator()));

        return result;
    }

    public void prepareRun(ExecutionContext executionContext) {
        for (ExtensionScript script : extensionScripts) {
            script.prepareRun(executionContext);
        }
    }

    public void completeRun(Execution execution) {
        for (ExtensionScript script : extensionScripts) {
            script.completeRun(execution);
        }
    }

    public void prepareMock(MockContext mockContext) {
        for (ExtensionScript script : extensionScripts) {
            script.prepareMock(mockContext);
        }
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

    public Locker getLocker() {
        return locker;
    }
}
