package com.wttech.aem.contentor.core.assist;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Suggestion implements Serializable {

    @JsonProperty("k")
    private final String kind;

    @JsonProperty("v")
    private final String value;

    @JsonProperty("i")
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
