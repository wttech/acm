package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;

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
