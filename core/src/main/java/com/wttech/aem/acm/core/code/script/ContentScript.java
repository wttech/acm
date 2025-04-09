package com.wttech.aem.acm.core.code.script;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.acl.AclGroovy;
import com.wttech.aem.acm.core.code.*;
import com.wttech.aem.acm.core.format.Formatter;
import com.wttech.aem.acm.core.replication.Activator;
import com.wttech.aem.acm.core.repo.Repository;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentScript {

    private final Executable executable;

    private final Script script;

    private final Arguments arguments = new Arguments();

    public ContentScript(Executable executable) {
        this.executable = executable;
        this.script = parseScript(executable);
    }

    private Script parseScript(Executable executable) {
        GroovyShell shell = ScriptUtils.createShell(new ContentScriptSyntax());
        Script script = shell.parse(executable.getContent(), ContentScriptSyntax.MAIN_CLASS);
        if (script == null) {
            throw new AcmException(String.format("Content script '%s' cannot be parsed!", executable.getId()));
        }
        return script;
    }

    public void prepare(ExecutionContext context) {
        try {
            script.getBinding().setVariable("args", arguments);
            script.getBinding().setVariable("condition", new Condition(context));
            script.getBinding().setVariable("log", createLogger(context.getExecutable()));
            script.getBinding().setVariable("out", new CodePrintStream(context));
            script.getBinding().setVariable("resourceResolver", context.getResourceResolver());
            script.getBinding().setVariable("osgi", context.getOsgiContext());
            script.getBinding().setVariable("repository", new Repository(context.getResourceResolver()));
            script.getBinding().setVariable("acl", new AclGroovy(context.getResourceResolver()));
            script.getBinding().setVariable("formatter", new Formatter());
            script.getBinding()
                    .setVariable(
                            "activator",
                            new Activator(
                                    context.getResourceResolver(),
                                    context.getOsgiContext().getReplicator()));
        } catch (Exception e) {
            throw new AcmException(
                    String.format(
                            "Content script '%s' cannot be prepared!",
                            context.getExecutable().getId()),
                    e);
        }
    }

    private Logger createLogger(Executable executable) {
        return LoggerFactory.getLogger(String.format("%s(%s)", getClass().getName(), executable.getId()));
    }

    public void describe() {
        try {
            script.invokeMethod(ContentScriptSyntax.Method.DESCRIBE.givenName, null);
        } catch (MissingMethodException e) {
            // ignore
        }
    }

    public boolean canRun() {
        return (Boolean) script.invokeMethod(ContentScriptSyntax.Method.CHECK.givenName, null);
    }

    public void run() {
        script.invokeMethod(ContentScriptSyntax.Method.RUN.givenName, null);
    }

    public Executable getExecutable() {
        return executable;
    }

    public Arguments getArguments() {
        return arguments;
    }

    public void variable(String name, Object value) {
        script.getBinding().setVariable(name, value);
    }
}
