package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.code.arg.*;
import com.wttech.aem.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
import java.io.Serializable;
import java.util.LinkedHashMap;

public class Arguments extends LinkedHashMap<String, Argument<?>> implements Serializable {

    private final ExecutionContext context;

    public Arguments(ExecutionContext context) {
        super();
        this.context = context;
    }

    private void add(Argument<?> argument) {
        put(argument.getName(), argument);
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
