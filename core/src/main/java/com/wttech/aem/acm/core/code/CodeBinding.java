package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.acl.Acl;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.replication.Activator;
import groovy.lang.Binding;
import java.io.PrintStream;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeBinding {

    private final Logger log;

    private final PrintStream out;

    private final ResourceResolver resourceResolver;

    private final Acl acl;

    private final Activator activator;

    private final OsgiContext osgi;

    private final Condition condition;

    public CodeBinding(ExecutionContext context) {
        this.log = createLogger(context.getExecutable());
        this.out = new CodePrintStream(context);
        this.resourceResolver = context.getResourceResolver();
        this.acl = new Acl(resourceResolver);
        this.activator = new Activator(
                context.getResourceResolver(), context.getOsgiContext().getReplicator());
        this.osgi = context.getOsgiContext();
        this.condition = new Condition(context);
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), executable.getId()));
    }

    public Binding toBinding() {
        Binding result = new Binding();
        result.setVariable(Variable.LOG.varName(), log);
        result.setVariable(Variable.OUT.varName(), out);
        result.setVariable(Variable.RESOURCE_RESOLVER.varName(), resourceResolver);
        result.setVariable(Variable.ACL.varName(), acl);
        result.setVariable(Variable.ACTIVATOR.varName(), activator);
        result.setVariable(Variable.OSGI.varName(), osgi);
        result.setVariable(Variable.CONDITION.varName(), condition);
        return result;
    }
}
