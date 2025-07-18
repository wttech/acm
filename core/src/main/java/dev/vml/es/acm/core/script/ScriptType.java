package dev.vml.es.acm.core.script;

import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public enum ScriptType {
    MANUAL(ScriptRepository.ROOT + "/manual"),
    ENABLED(ScriptRepository.ROOT + "/auto/enabled"),
    DISABLED(ScriptRepository.ROOT + "/auto/disabled"),
    MOCK(ScriptRepository.ROOT + "/mock"),
    EXTENSION(ScriptRepository.ROOT + "/extension"),
    TEMPLATE(ScriptRepository.ROOT + "/template");

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

    public boolean statsSupported() {
        return this == MANUAL || this == ENABLED || this == DISABLED;
    }

    public String root() {
        return root;
    }
}
