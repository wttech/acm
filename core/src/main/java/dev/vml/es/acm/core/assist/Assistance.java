package dev.vml.es.acm.core.assist;

import java.io.Serializable;
import java.util.List;

public class Assistance implements Serializable {

    private final List<Suggestion> suggestions;

    public Assistance(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }
}
