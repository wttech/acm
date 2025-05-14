package com.vml.es.aem.acm.core.code.script;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.code.*;
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
        Script script = shell.parse(
                executionContext.getExecutable().getContent(),
                ContentScriptSyntax.MAIN_CLASS,
                executionContext.getCodeContext().getBinding());
        if (script == null) {
            throw new AcmException(String.format(
                    "Content script '%s' cannot be parsed!",
                    executionContext.getExecutable().getId()));
        }
        return script;
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
