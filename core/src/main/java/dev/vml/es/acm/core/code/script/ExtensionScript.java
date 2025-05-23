package dev.vml.es.acm.core.code.script;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.CodeContext;
import dev.vml.es.acm.core.code.Executable;
import dev.vml.es.acm.core.code.Execution;
import dev.vml.es.acm.core.code.ExecutionContext;
import dev.vml.es.acm.core.mock.MockContext;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;

public class ExtensionScript {

    private final Executable extensionScript;

    private final Script script;

    public ExtensionScript(CodeContext codeContext, Executable extensionScript) {
        this.extensionScript = extensionScript;
        this.script = ScriptUtils.createShell(new ExtensionScriptSyntax())
                .parse(this.extensionScript.getContent(), ExtensionScriptSyntax.MAIN_CLASS, codeContext.getBinding());
    }

    public void prepareRun(ExecutionContext executionContext) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.PREPARE_RUN.givenName, executionContext);
        } catch (Exception e) {
            throw new AcmException(
                    String.format(
                            "Cannot prepare content script context with extension script '%s'!",
                            extensionScript.getId()),
                    e);
        }
    }

    public void completeRun(Execution execution) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.COMPLETE_RUN.givenName, execution);
        } catch (Exception e) {
            throw new AcmException(
                    String.format(
                            "Cannot complete execution '%s' with extension script '%s'!",
                            execution.getId(), extensionScript.getId()),
                    e);
        }
    }

    public void prepareMock(MockContext mockContext) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.PREPARE_MOCK.givenName, mockContext);
        } catch (MissingMethodException e) {
            // ignore as the method is optional
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot prepare mock context with extension script '%s'!", extensionScript.getId()),
                    e);
        }
    }
}
