package com.wttech.aem.acm.core.code;

public abstract class Argument<V> {

    private final String name;

    private final ArgumentType type;

    private V value;

    private String label;

    private String group = "default";

    private boolean required;

    public Argument(String name, ArgumentType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ArgumentType getType() {
        return type;
    }

    public V getValue() {
        return value;
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
