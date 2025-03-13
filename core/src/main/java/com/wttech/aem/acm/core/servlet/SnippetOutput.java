package com.wttech.aem.acm.core.servlet;

import com.wttech.aem.acm.core.snippet.Snippet;
import java.io.Serializable;
import java.util.List;

public class SnippetOutput implements Serializable {

    public List<Snippet> list;

    public SnippetOutput(List<Snippet> snippets) {
        this.list = snippets;
    }

    public List<Snippet> getList() {
        return list;
    }
}
