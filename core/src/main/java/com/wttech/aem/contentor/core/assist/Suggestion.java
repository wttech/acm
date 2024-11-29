package com.wttech.aem.contentor.core.assist;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public interface Suggestion extends Serializable {

    @JsonProperty("k")
    String getKind();

    @JsonProperty("v")
    String getValue();

    @JsonProperty("i")
    String getInfo();
}
