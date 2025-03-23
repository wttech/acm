package com.wttech.aem.acm.core.code;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Arguments implements Serializable {

    private final ExecutionContext context;

    private final Map<String, Argument<?>> arguments = new HashMap<>();

    public Arguments(ExecutionContext context) {
        this.context = context;
    }

    public Map<String, Argument<?>> getArguments() {
        return arguments;
    }

    public void checkbox(String name, String label, boolean defaultValue) {
        Argument<Boolean> arg = new Argument<>(name);
        arg.setMetadata("label", label);
        arg.setMetadata("render", "checkbox");
        arg.setValue(defaultValue);
        arguments.put(name, arg);
    }

    public <V> void radio(String name, String label, Map<String, Object> options, V defaultValue) {
        Argument<V> arg = new Argument<>(name);
        arg.setMetadata("label", label);
        arg.setMetadata("render", "radio");
        arg.setMetadata("options", options);
        arg.setValue(defaultValue);
        arguments.put(name, arg);
    }

    public <V> void select(String name, String label, Map<String, Object> options, V defaultValue) {
        Argument<V> arg = new Argument<>(name);
        arg.setMetadata("label", label);
        arg.setMetadata("render", "select");
        arg.setMetadata("options", options);
        arg.setValue(defaultValue);
        arguments.put(name, arg);
    }

    public void text(String name, String label, String defaultValue) {
        Argument<String> arg = new Argument<>(name);
        arg.setMetadata("label", label);
        arg.setMetadata("render", "text");
        arg.setValue(defaultValue);
        arguments.put(name, arg);
    }

    public void richtext(String name, String label, String language, String defaultValue) {
        Argument<String> arg = new Argument<>(name);
        arg.setMetadata("label", label);
        arg.setMetadata("render", "richtext");
        arg.setMetadata("language", language);
        arg.setValue(defaultValue);
        arguments.put(name, arg);
    }

    public void json(String name, String label, String defaultValue) {
        richtext(name, label, "json", defaultValue);
    }

    public void csv(String name, String label, String defaultValue) {
        richtext(name, label, "csv", defaultValue);
    }

    public void integer(String name, String label, int defaultValue) {
        Argument<Integer> arg = new Argument<>(name);
        arg.setMetadata("label", label);
        arg.setMetadata("render", "integer");
        arg.setValue(defaultValue);
        arguments.put(name, arg);
    }
}
