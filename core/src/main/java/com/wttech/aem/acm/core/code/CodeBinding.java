package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.acl.Acl;
import com.wttech.aem.acm.core.acl.AclGroovy;
import com.wttech.aem.acm.core.format.Formatter;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.replication.Activator;
import com.wttech.aem.acm.core.repo.Repository;
import groovy.lang.Binding;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeBinding {

    private final Arguments arguments;

    private Map<String, CodeVariable<?>> variables = new HashMap<>();

    public CodeBinding(ExecutionContext context) {
        this.arguments = new Arguments(context);

        registerVariables(context);
    }

    private void registerVariables(ExecutionContext context) {
        registerVariable(new CodeVariable<>("args", Arguments.class, () -> arguments, "Arguments passed to the script."));
        registerVariable(new CodeVariable<>("log", Logger.class, () -> createLogger(context.getExecutable()), null));
        registerVariable(new CodeVariable<>("out", PrintStream.class, () -> new CodePrintStream(context), "Output stream"));
        registerVariable(new CodeVariable<>("condition", Condition.class, () -> new Condition(context), "Facade for easy condition checks"));
        registerVariable(new CodeVariable<>("osgi", OsgiContext.class, context::getOsgiContext, "Facade for OSGi operations"));
        registerVariable(new CodeVariable<>("resourceResolver", ResourceResolver.class, context::getResourceResolver, "Sling Resource Resolver"));
        registerVariable(new CodeVariable<>("repository", Repository.class, () -> new Repository(context.getResourceResolver()), "Facade for idempotent repository operations"));
        registerVariable(new CodeVariable<>("acl", Acl.class, () -> new AclGroovy(context.getResourceResolver()), "Facade for ACL operations"));
        registerVariable(new CodeVariable<>("formatter", Formatter.class, Formatter::new , "Facade for input/output format operations (JSON/YML/CSV)"));
        registerVariable(new CodeVariable<>("activator", Activator.class, () -> new Activator(context.getResourceResolver(), context.getOsgiContext().getReplicator()), "Facade for replication operations"));
    }

    public void registerVariable(CodeVariable<?> variable) {
        if (variables.containsKey(variable.getName())) {
            throw new IllegalArgumentException(
                    String.format("Code binding variable '%s' already exists!", variable.getName()));
        }
        variables.put(variable.getName(), variable);
    }

    public Arguments getArguments() {
        return arguments;
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), executable.getId()));
    }

    public Binding toBinding() {
        Binding result = new Binding();
        variables.forEach((k, v) -> {
            result.setVariable(k, v.getSupplier().get());
        });
        return result;
    }
}
