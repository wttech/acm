package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Argument<V> {

    private final String name;

    private final ArgumentType type;

    private final Class<?> valueType;

    private V value;

    private String label;

    private String description;

    private String group = "general";

    private boolean required = true;

    private String validator;

    public Argument(String name, ArgumentType type, Class<?> valueType) {
        this.name = name;
        this.type = type;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public ArgumentType getType() {
        return type;
    }

    @JsonIgnore
    public Class<?> getValueType() {
        return valueType;
    }

    public V getValue() {
        return value;
    }

    public V value() {
        return getValue();
    }

    public void setValue(V value) {
        this.value = value;
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void required() {
        this.required = true;
    }

    public void optional() {
        this.required = false;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getValidator() {
        return validator;
    }

    public void setValidator(String validator) {
        this.validator = validator;
    }
}
