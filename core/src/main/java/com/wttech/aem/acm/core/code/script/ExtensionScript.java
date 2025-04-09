package com.wttech.aem.acm.core.code.script;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.code.Execution;
import com.wttech.aem.acm.core.code.ExecutionContext;
import groovy.lang.Script;

public class ExtensionScript {

    private final ExecutionContext context;

    private final Script script;

    public ExtensionScript(ExecutionContext context) {
        this.context = context;
        this.script = ScriptUtils.createShell(new ExtensionScriptSyntax())
                .parse(context.getExecutable().getContent(), ExtensionScriptSyntax.MAIN_CLASS);
    }

    public void extend(ContentScript contentScript) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.EXTEND.givenName, contentScript);
        } catch (Exception e) {
            throw new AcmException(
                    String.format(
                            "Cannot extend shell with extension script '%s'",
                            context.getExecutable().getId()),
                    e);
        }
    }

    public void complete(Execution execution) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.COMPLETE.givenName, execution);
        } catch (Exception e) {
            throw new AcmException(
                    String.format(
                            "Cannot complete execution with extension script '%s'",
                            context.getExecutable().getId()),
                    e);
        }
    }
}
