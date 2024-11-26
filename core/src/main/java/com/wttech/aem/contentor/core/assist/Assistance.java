package com.wttech.aem.contentor.core.assist;

import java.io.Serializable;
import java.util.List;

public class Assistance implements Serializable {

    private final String word;

    private final List<Suggestion> suggestions;

    public Assistance(String code, List<Suggestion> suggestions) {
        this.word = code;
        this.suggestions = suggestions;
    }

    public String getWord() {
        return word;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }
}
