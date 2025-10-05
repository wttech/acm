package dev.vml.es.acm.core.code;

import java.io.Serializable;

public abstract class Output implements Serializable {

    private String name;

    private String label;

    private String description;

    public Output() {
        // for deserialization
    }

    public Output(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
