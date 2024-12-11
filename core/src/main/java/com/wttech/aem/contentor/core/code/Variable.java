package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.acl.Acl;
import com.wttech.aem.contentor.core.script.Script;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;

import java.io.PrintStream;

public enum Variable {
    OUT("out", PrintStream.class.getName()),
    LOG("log", Logger.class.getName()),
    RESOURCE_RESOLVER("resourceResolver", ResourceResolver.class.getName()),
    ACL("acl", Acl.class.getName()),
    CONDITION("condition", Condition.class.getName());

    final String varName;

    final String className;

    Variable(String varName, String className) {
        this.varName = varName;
        this.className = className;
    }

    public String varName() {
        return varName;
    }

    public String typeName() {
        return className;
    }
}
