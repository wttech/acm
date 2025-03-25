package com.wttech.aem.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wttech.aem.acm.core.code.arg.*;
import com.wttech.aem.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class Arguments implements Serializable {

    private final ExecutionContext context;

    private final Map<String, Argument<?>> definitions = new LinkedHashMap<>();

    public Arguments(ExecutionContext context) {
        super();
        this.context = context;
    }

    private void add(Argument<?> argument) {
        definitions.put(argument.getName(), argument);
    }

    @JsonAnyGetter
    public Map<String, Argument<?>> getDefinitions() {
        return definitions;
    }

    @JsonIgnore
    public ValueMap getValues() {
        return new ValueMapDecorator(
                definitions.values().stream().collect(Collectors.toMap(Argument::getName, Argument::getValue)));
    }

    public <T> T value(String name, Class<T> type) {
        return getValues().get(name, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T value(String name) {
        return (T) getValues().get(name);
    }

    public void toggle(String name) {
        toggle(name, null);
    }

    public void toggle(String name, Closure<ToggleArgument> options) {
        ToggleArgument argument = new ToggleArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void string(String name) {
        string(name, null);
    }

    public void string(String name, Closure<StringArgument> options) {
        StringArgument argument = new StringArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void text(String name) {
        text(name, null);
    }

    public void text(String name, Closure<TextArgument> options) {
        TextArgument argument = new TextArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void select(String name) {
        select(name, null);
    }

    public <V> void select(String name, Closure<SelectArgument<V>> options) {
        SelectArgument<V> argument = new SelectArgument<>(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void integerNumber(String name) {
        integerNumber(name, null);
    }

    public void integerNumber(String name, Closure<IntegerArgument> options) {
        IntegerArgument argument = new IntegerArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void doubleNumber(String name) {
        doubleNumber(name, null);
    }

    public void doubleNumber(String name, Closure<DoubleArgument> options) {
        DoubleArgument argument = new DoubleArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }
}
