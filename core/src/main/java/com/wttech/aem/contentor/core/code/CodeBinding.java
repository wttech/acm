package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.acl.Acl;
import com.wttech.aem.contentor.core.script.Script;
import groovy.lang.Binding;
import java.io.OutputStream;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeBinding {

    private final Logger log;

    private final OutputStream out;

    private final ResourceResolver resourceResolver;

    private final Acl acl;

    private final Script script;

    public CodeBinding(Executable executable, ExecutionOptions executionOptions) {
        this.log = createLogger(executable);
        this.out = executionOptions.getOutputStream();
        this.resourceResolver = executionOptions.getResourceResolver();
        this.acl = new Acl(resourceResolver);
        this.script = executionOptions.getScript();
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
        result.setVariable(Variable.SCRIPT.varName(), script);
        return result;
    }
}
