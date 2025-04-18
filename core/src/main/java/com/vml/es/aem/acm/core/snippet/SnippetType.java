package com.vml.es.aem.acm.core.snippet;

public enum SnippetType {
    AVAILABLE(SnippetRepository.ROOT + "/available");

    private final String root;

    SnippetType(String root) {
        this.root = root;
    }

    public String root() {
        return root;
    }
}
