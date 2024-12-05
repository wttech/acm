package com.wttech.aem.contentor.core.snippet;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public enum SnippetType {
    AVAILABLE(SnippetRepository.ROOT + "/available");

    private final String root;

    SnippetType(String root) {
        this.root = root;
    }

    public String root() {
        return root;
    }

    public static Optional<String> pathWithoutRoot(String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }
        for (SnippetType type : SnippetType.values()) {
            if (StringUtils.startsWith(path, type.root() + "/")) {
                return Optional.of(StringUtils.removeStart(path, type.root() + "/"));
            }
        }
        return Optional.empty();
    }
}
