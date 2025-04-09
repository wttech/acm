package com.wttech.aem.acm.core.code.script;

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

    private final Arguments arguments = new Arguments();

    private final Script script;

    public ContentScript(ExecutionContext context) {
        GroovyShell shell = ScriptUtil.createShell(new ContentScriptSyntax());

        Script script = shell.parse(context.getExecutable().getContent(), ContentScriptSyntax.MAIN_CLASS);
        prepareScript(script, context);
        context.getExtender().extend(this);
        this.script = script;
    }

    private void prepareScript(Script script, ExecutionContext context) {
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

    public Arguments getArguments() {
        return arguments;
    }

    public void variable(String name, Object value) {
        script.getBinding().setVariable(name, value);
    }
}
