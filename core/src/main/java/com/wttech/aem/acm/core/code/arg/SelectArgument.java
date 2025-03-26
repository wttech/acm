package com.wttech.aem.acm.core.code.arg;

import com.wttech.aem.acm.core.code.Argument;
import com.wttech.aem.acm.core.code.ArgumentType;
import java.util.LinkedHashMap;
import java.util.Map;

public class SelectArgument<V> extends Argument<V> {

    private Map<String, V> options = new LinkedHashMap<>();

    public SelectArgument(String name) {
        super(name, ArgumentType.SELECT);
    }

    public Map<String, V> getOptions() {
        return options;
    }

    public void setOptions(Map<String, V> options) {
        this.options = options;
    }
}
