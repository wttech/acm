package dev.vml.es.acm.core.code.script;

import dev.vml.es.acm.core.code.*;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;

public class ContentScript {

    private final ExecutionContext executionContext;

    private final Script script;

    public ContentScript(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.script = parseScript();
    }

    private Script parseScript() {
        GroovyShell shell = ScriptUtils.createShell(new ContentScriptSyntax());
        return shell.parse(
                executionContext.getExecutable().getContent(),
                ContentScriptSyntax.MAIN_CLASS,
                executionContext.getCodeContext().getBinding());
    }

    public void describe() {
        try {
            script.invokeMethod(ContentScriptSyntax.Method.DESCRIBE.givenName, null);
        } catch (MissingMethodException e) {
            // ignore as the method is optional
        }
    }

    public boolean canRun() {
        return (Boolean) script.invokeMethod(ContentScriptSyntax.Method.CHECK.givenName, null);
    }

    public void run() {
        script.invokeMethod(ContentScriptSyntax.Method.RUN.givenName, null);
    }
}
