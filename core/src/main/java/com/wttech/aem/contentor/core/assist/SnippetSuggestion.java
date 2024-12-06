package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.snippet.Snippet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnippetSuggestion implements Suggestion {

    private static final Logger LOG = LoggerFactory.getLogger(SnippetSuggestion.class);

    private final Snippet snippet;

    public SnippetSuggestion(Snippet snippet) {
        this.snippet = snippet;
    }

    @Override
    public String getKind() {
        return "snippet";
    }

    @Override
    public String getLabel() {
        return snippet.getName();
    }

    @Override
    public String getInsertText() {
        try {
            return snippet.getContent();
        } catch (ContentorException e) {
            LOG.error("Cannot read snippet content", e);
            return null;
        }
    }

    @Override
    public String getInfo() {
        return String.format("ID: %s", snippet.getId());
    }
}
