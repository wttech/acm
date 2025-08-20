package dev.vml.es.acm.core.servlet;

import dev.vml.es.acm.core.script.Script;
import java.io.Serializable;

public class ScriptOutput implements Serializable {

    private final Script script;

    public ScriptOutput(Script script) {
        this.script = script;
    }

    public Script getScript() {
        return script;
    }
}
