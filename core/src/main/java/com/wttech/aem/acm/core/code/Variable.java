package com.wttech.aem.acm.core.code;

import java.io.Serializable;

public class Variable implements Serializable {

    private String name;

    private String type;

    private String documentation;

    public Variable(String name, Class<?> type) {
        this.name = name;
        this.type = type.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
