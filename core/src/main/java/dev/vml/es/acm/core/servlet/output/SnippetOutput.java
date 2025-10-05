package dev.vml.es.acm.core.servlet.output;

import dev.vml.es.acm.core.snippet.Snippet;
import java.io.Serializable;
import java.util.List;

public class SnippetOutput implements Serializable {

    private List<Snippet> list;

    public SnippetOutput(List<Snippet> snippets) {
        this.list = snippets;
    }

    public List<Snippet> getList() {
        return list;
    }
}
