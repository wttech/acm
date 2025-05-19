package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;

public class TextArgument extends Argument<String> {

    private String language;

    public TextArgument(String name) {
        super(name, ArgumentType.TEXT);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
