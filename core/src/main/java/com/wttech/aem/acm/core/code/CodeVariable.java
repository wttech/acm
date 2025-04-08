package com.wttech.aem.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.function.Supplier;

public class CodeVariable<T> implements Serializable {

    private final String name;

    private final Class<T> type;

    private final String documentation;

    @JsonIgnore
    private final Supplier<T> supplier;

    public CodeVariable(String name, Class<T> type, Supplier<T> supplier, String documentation) {
        this.name = name;
        this.type = type;
        this.supplier = supplier;
        this.documentation = documentation;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public Supplier<?> getSupplier() {
        return supplier;
    }

    public String getDocumentation() {
        return documentation;
    }
}
