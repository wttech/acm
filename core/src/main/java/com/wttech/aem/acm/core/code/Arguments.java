package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.code.arg.*;
import com.wttech.aem.acm.core.util.GroovyUtils;
import groovy.lang.Closure;
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

    private void add(Argument<?> argument) {
        arguments.put(argument.getName(), argument);
    }

    public void toggle(String name, Closure<ToggleArgument> options) {
        ToggleArgument argument = new ToggleArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void string(String name, Closure<StringArgument> options) {
        StringArgument argument = new StringArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void text(String name, Closure<TextArgument> options) {
        TextArgument argument = new TextArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void select(String name, Closure<SelectArgument> options) {
        SelectArgument argument = new SelectArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void integerNumber(String name, Closure<IntegerArgument> options) {
        IntegerArgument argument = new IntegerArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void doubleNumber(String name, Closure<DoubleArgument> options) {
        DoubleArgument argument = new DoubleArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }
}
