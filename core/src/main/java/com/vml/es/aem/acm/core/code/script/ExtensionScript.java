package com.vml.es.aem.acm.core.code.script;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.code.Executable;
import com.vml.es.aem.acm.core.code.Execution;
import com.vml.es.aem.acm.core.code.ExecutionContext;
import groovy.lang.Script;

public class ExtensionScript {

    private final Executable executable;

    private final Script script;

    public ExtensionScript(ExecutionContext contentContext, Executable extensionScript) {
        this.executable = extensionScript;
        this.script = ScriptUtils.createShell(new ExtensionScriptSyntax())
                .parse(executable.getContent(), ExtensionScriptSyntax.MAIN_CLASS, contentContext.getBinding());
    }

    public void prepare(ExecutionContext executionContext) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.PREPARE.givenName, executionContext);
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot extend content script with extension script '%s'!", executable.getId()), e);
        }
    }

    public void complete(Execution execution) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.COMPLETE.givenName, execution);
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot complete execution with extension script '%s'", executable.getId()), e);
        }
    }
}
