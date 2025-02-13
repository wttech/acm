package com.wttech.aem.contentor.core.script;

import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public enum ScriptType {
    ENABLED(ScriptRepository.ROOT + "/enabled"),
    DISABLED(ScriptRepository.ROOT + "/disabled");

    private final String root;

    ScriptType(String root) {
        this.root = root;
    }

    public static Optional<ScriptType> of(String name) {
        return Arrays.stream(ScriptType.values())
                .filter(t -> t.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public static Optional<ScriptType> byPath(String path) {
        return Arrays.stream(ScriptType.values())
                .filter(t -> StringUtils.startsWith(path, t.root() + "/"))
                .findFirst();
    }

    public String enforcePath(String path) {
        String subPath = path;
        for (ScriptType value : values()) {
            subPath = StringUtils.removeStart(subPath, value.root() + "/");
        }
        return root + "/" + subPath;
    }

    public String root() {
        return root;
    }
}
