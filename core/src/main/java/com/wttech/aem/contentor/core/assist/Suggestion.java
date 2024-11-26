package com.wttech.aem.contentor.core.assist;

import java.io.Serializable;

public class Suggestion implements Serializable {

    private final String kind;

    private final String value;

    private final String info;

    public Suggestion(String kind, String value) {
        this.kind = kind;
        this.value = value;
        this.info = null;
    }

    public Suggestion(String kind, String value, String info) {
        this.kind = kind;
        this.value = value;
        this.info = info;
    }

    public String getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    public String getInfo() {
        return info;
    }
}
