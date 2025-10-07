package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = FileOutput.class) // TODO remove 'defaultImpl'
@JsonSubTypes({
    @JsonSubTypes.Type(value = FileOutput.class, name = "FILE"),
    @JsonSubTypes.Type(value = TextOutput.class, name = "TEXT")
})
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

    @JsonIgnore
    public abstract OutputType getType();

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
