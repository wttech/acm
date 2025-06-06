package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.code.arg.*;
import dev.vml.es.acm.core.util.GroovyUtils;
import dev.vml.es.acm.core.util.TypeUtils;
import dev.vml.es.acm.core.util.TypeValueMap;
import groovy.lang.Closure;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.api.resource.ValueMap;

public class Arguments implements Serializable {

    private final Map<String, Argument<?>> definitions = new LinkedHashMap<>();

    public Arguments() {
        super();
    }

    public Argument<?> get(String name) {
        Argument<?> argument = definitions.get(name);
        if (argument == null) {
            throw new IllegalArgumentException(String.format("Argument '%s' is not defined!", name));
        }
        return argument;
    }

    private void add(Argument<?> argument) {
        if (definitions.containsKey(argument.getName())) {
            throw new IllegalArgumentException(
                    String.format("Argument with name '%s' is already defined!", argument.getName()));
        }
        definitions.put(argument.getName(), argument);
    }

    @JsonAnyGetter
    public Map<String, Argument<?>> getDefinitions() {
        return definitions;
    }

    @JsonIgnore
    public ValueMap getValues() {
        Map<String, Object> props = new HashMap<>();
        for (Argument<?> argument : definitions.values()) {
            props.put(argument.getName(), argument.getValue());
        }
        return new TypeValueMap(props);
    }

    @JsonIgnore
    public ValueMap values() {
        return getValues();
    }

    public <T> T getValue(String name, Class<T> type) {
        return getValues().get(name, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name) {
        return (T) getValues().get(name);
    }

    public <T> T value(String name, Class<T> type) {
        return getValue(name, type);
    }

    public <T> T value(String name) {
        return getValue(name);
    }

    public void setValues(ArgumentValues arguments) {
        arguments.forEach((name, value) -> {
            Argument<?> argument = get(name);
            setValue(argument, value);
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void setValue(Argument<T> argument, Object value) {
        Class<?> valueType = argument.getValueType();
        if (valueType == null) {
            argument.setValue((T) value);
        } else {
            Optional<?> convertedValue = TypeUtils.convert(value, valueType, true);
            if (convertedValue.isPresent()) {
                argument.setValue((T) convertedValue.get());
            } else {
                argument.setValue((T) value);
            }
        }
    }

    public void bool(String name) {
        bool(name, null);
    }

    public void bool(String name, Closure<BoolArgument> options) {
        BoolArgument argument = new BoolArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void dateTime(String name) {
        dateTime(name, null);
    }

    public void dateTime(String name, Closure<DateTimeArgument> options) {
        DateTimeArgument argument = new DateTimeArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void date(String name) {
        date(name, null);
    }

    public void date(String name, Closure<DateArgument> options) {
        DateArgument argument = new DateArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void time(String name) {
        time(name, null);
    }

    public void time(String name, Closure<TimeArgument> options) {
        TimeArgument argument = new TimeArgument(name);
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

    public void multiSelect(String name) {
        multiSelect(name, null);
    }

    public <V> void multiSelect(String name, Closure<MultiSelectArgument<V>> options) {
        MultiSelectArgument<V> argument = new MultiSelectArgument<>(name);
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

    public void color(String name) {
        color(name, null);
    }

    public void color(String name, Closure<ColorArgument> options) {
        ColorArgument argument = new ColorArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void integerRange(String name, Closure<IntegerRangeArgument> options) {
        IntegerRangeArgument argument = new IntegerRangeArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void decimalRange(String name) {
        decimalRange(name, null);
    }

    public void decimalRange(String name, Closure<DecimalRangeArgument> options) {
        DecimalRangeArgument argument = new DecimalRangeArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void decimalNumber(String name) {
        decimalNumber(name, null);
    }

    public void decimalNumber(String name, Closure<DecimalArgument> options) {
        DecimalArgument argument = new DecimalArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void path(String name) {
        text(name, null);
    }

    public void path(String name, Closure<PathArgument> options) {
        PathArgument argument = new PathArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void file(String name) {
        file(name, null);
    }

    public void file(String name, Closure<FileArgument> options) {
        FileArgument argument = new FileArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }

    public void multiFile(String name) {
        multiFile(name, null);
    }

    public void multiFile(String name, Closure<MultiFileArgument> options) {
        MultiFileArgument argument = new MultiFileArgument(name);
        GroovyUtils.with(argument, options);
        add(argument);
    }
}
