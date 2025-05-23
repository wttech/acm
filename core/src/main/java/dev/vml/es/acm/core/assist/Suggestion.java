package dev.vml.es.acm.core.assist;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public interface Suggestion extends Serializable {

    @JsonProperty("k")
    String getKind();

    @JsonProperty("l")
    String getLabel();

    @JsonProperty("it")
    String getInsertText();

    @JsonProperty("i")
    String getInfo();
}
