package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.code.script.ContentScript;
import com.wttech.aem.acm.core.code.script.ExtensionScript;
import com.wttech.aem.acm.core.script.ScriptRepository;
import com.wttech.aem.acm.core.script.ScriptType;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;

public class Extender {

    private final List<ExtensionScript> scripts;

    public Extender(Executor executor, ResourceResolver resourceResolver) {
        this.scripts = new ScriptRepository(resourceResolver)
                .findAll(ScriptType.EXTENSION)
                .map(s -> new ExtensionScript(executor.createContext(s, resourceResolver)))
                .collect(Collectors.toList());
    }

    public void extend(ContentScript contentScript) {
        for (ExtensionScript script : scripts) {
            script.extend(contentScript);
        }
    }

    public void complete(Execution execution) {
        for (ExtensionScript script : scripts) {
            script.complete(execution);
        }
    }
}
