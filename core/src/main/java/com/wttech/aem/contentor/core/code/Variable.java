package com.wttech.aem.contentor.core.code;

public enum Variable {
    RESOURCE_RESOLVER("resourceResolver", "org.apache.sling.api.resource.ResourceResolver"),
    OUT("out", "java.io.PrintStream"),
    LOG("log", "org.slf4j.Logger"),;

    final String varName;

    final String className;

    Variable(String varName, String className) {
        this.varName = varName;
        this.className = className;
    }

    public String bindingName() {
        return varName;
    }

    public String typeName() {
        return className;
    }
}
