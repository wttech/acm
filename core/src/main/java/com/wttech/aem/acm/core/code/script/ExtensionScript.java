package com.wttech.aem.acm.core.code.script;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.code.Executable;
import com.wttech.aem.acm.core.code.Execution;
import groovy.lang.Script;

public class ExtensionScript {

    private final Executable executable;

    private final Script script;

    public ExtensionScript(Executable executable) {
        this.executable = executable;
        this.script = ScriptUtils.createShell(new ExtensionScriptSyntax())
                .parse(executable.getContent(), ExtensionScriptSyntax.MAIN_CLASS);
    }

    public void extend(ContentScript contentScript) {
        try {
            script.invokeMethod(ExtensionScriptSyntax.Method.EXTEND.givenName, contentScript);
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
