package com.wttech.aem.contentor.core.script;

public enum ScriptType {
    ENABLED(ScriptRepository.ROOT + "/enabled"),
    DISABLED(ScriptRepository.ROOT + "/disabled");

    private final String root;

    ScriptType(String root) {
        this.root = root;
    }

    public String root() {
        return root;
    }
}
