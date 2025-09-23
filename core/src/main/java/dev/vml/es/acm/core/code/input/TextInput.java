package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;

public class TextInput extends Input<String> {

    private String language;

    public TextInput(String name) {
        super(name, InputType.TEXT, String.class);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
