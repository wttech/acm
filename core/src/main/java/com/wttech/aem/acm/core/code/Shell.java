package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.acl.AclGroovy;
import com.wttech.aem.acm.core.format.Formatter;
import com.wttech.aem.acm.core.replication.Activator;
import com.wttech.aem.acm.core.repo.Repository;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shell {

    private final GroovyShell groovyShell;

    private final Arguments arguments = new Arguments();

    public Shell(GroovyShell groovyShell) {
        this.groovyShell = groovyShell;
    }

    public Script parseCode(String code, String fileName) throws CompilationFailedException {
        return groovyShell.parse(code, fileName);
    }

    public void setVariables(ExecutionContext context) {
        groovyShell.setVariable("args", arguments);
        groovyShell.setVariable("condition", new Condition(context));
        groovyShell.setVariable("log", createLogger(context.getExecutable()));
        groovyShell.setVariable("out", new CodePrintStream(context));
        groovyShell.setVariable("resourceResolver", context.getResourceResolver());
        groovyShell.setVariable("osgi", context.getOsgiContext());
        groovyShell.setVariable("repository", new Repository(context.getResourceResolver()));
        groovyShell.setVariable("acl", new AclGroovy(context.getResourceResolver()));
        groovyShell.setVariable("formatter", new Formatter());
        groovyShell.setVariable(
                "activator",
                new Activator(
                        context.getResourceResolver(), context.getOsgiContext().getReplicator()));
    }

    public Arguments getArguments() {
        return arguments;
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), executable.getId()));
    }
}
