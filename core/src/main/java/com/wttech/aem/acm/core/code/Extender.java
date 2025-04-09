package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.script.ScriptRepository;
import com.wttech.aem.acm.core.script.ScriptType;
import groovy.lang.Script;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;

public class Extender {

    private final Shell shell;

    private final List<Script> scripts;

    public Extender(Executor executor, ResourceResolver resourceResolver) {
        this.shell = executor.createExtensionShell();
        this.scripts = new ScriptRepository(resourceResolver)
                .findAll(ScriptType.EXTENSION)
                .map(s -> shell.parseCode(s.getContent(), s.getName()))
                .collect(Collectors.toList());
    }

    public void extend(Shell contentShell) {
        for (Script script : scripts) {
            try {
                script.invokeMethod(ExtensionCodeSyntax.EXTEND_RUN.givenName, contentShell);
            } catch (Exception e) {
                throw new AcmException(
                        String.format(
                                "Cannot extend shell with extension script '%s'",
                                script.getClass().getName()),
                        e);
            }
        }
    }

    public void complete(Execution execution) {
        for (Script script : scripts) {
            try {
                script.invokeMethod(ExtensionCodeSyntax.COMPLETE_RUN.givenName, execution);
            } catch (Exception e) {
                throw new AcmException(
                        String.format(
                                "Cannot complete execution with extension script '%s'",
                                script.getClass().getName()),
                        e);
            }
        }
    }
}
