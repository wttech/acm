package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.acl.Acl;
import groovy.lang.Binding;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

public class CodeBinding {

    private final Logger log;

    private final OutputStream out;

    private final ResourceResolver resourceResolver;

    private final Acl acl;

    private final Condition condition;

    public CodeBinding(Executable executable, ExecutionContext context) {
        this.log = createLogger(executable);
        this.out = context.getOutputStream();
        this.resourceResolver = context.getResourceResolver();
        this.acl = new Acl(resourceResolver);
        this.condition = new Condition(context);
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", Executor.class.getName(), executable.getId()));
    }

    public Binding toBinding() {
        Binding result = new Binding();
        result.setVariable(Variable.LOG.varName(), log);
        result.setVariable(Variable.OUT.varName(), out);
        result.setVariable(Variable.RESOURCE_RESOLVER.varName(), resourceResolver);
        result.setVariable(Variable.ACL.varName(), acl);
        result.setVariable(Variable.CONDITION.varName(), condition);
        return result;
    }
}
