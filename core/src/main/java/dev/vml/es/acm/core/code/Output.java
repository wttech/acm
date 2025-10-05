package dev.vml.es.acm.core.code;

import java.io.Serializable;

public abstract class Output implements Serializable {

    private String name;

    private String label;

    private String description;

    private OutputType type = OutputType.FILE; // TODO 'FILE'' here only for backward compatibility

    public Output() {
        // for deserialization
    }

    public Output(String name, OutputType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public OutputType getType() {
        return type;
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
