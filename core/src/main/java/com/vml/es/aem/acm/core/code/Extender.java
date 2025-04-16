package com.vml.es.aem.acm.core.code;

import com.vml.es.aem.acm.core.code.script.ExtensionScript;
import com.vml.es.aem.acm.core.script.ScriptRepository;
import com.vml.es.aem.acm.core.script.ScriptType;
import java.util.List;
import java.util.stream.Collectors;

public class Extender {

    private final List<ExtensionScript> scripts;

    public Extender(ExecutionContext contentContext) {
        this.scripts = new ScriptRepository(contentContext.getResourceResolver())
                .findAll(ScriptType.EXTENSION)
                .map(s -> new ExtensionScript(contentContext, s))
                .collect(Collectors.toList());

        prepare(contentContext);
    }

    public void prepare(ExecutionContext executionContext) {
        for (ExtensionScript script : scripts) {
            script.prepare(executionContext);
        }
    }

    public void complete(Execution execution) {
        for (ExtensionScript script : scripts) {
            script.complete(execution);
        }
    }
}
