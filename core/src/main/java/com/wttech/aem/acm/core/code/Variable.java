package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.acl.Acl;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.replication.Activator;
import java.io.PrintStream;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;

public enum Variable {
    OUT(
            "out",
            PrintStream.class
                    .getName()), // capturing output of 'groovy.lang.Script' requires 'out' to be 'java.io.PrintStream'
    LOG("log", Logger.class.getName()),
    ARGS("args", Arguments.class.getName()),
    CONDITION("condition", Condition.class.getName()),
    RESOURCE_RESOLVER("resourceResolver", ResourceResolver.class.getName()),
    ACL("acl", Acl.class.getName()),
    ACTIVATOR("activator", Activator.class.getName()),
    OSGI("osgi", OsgiContext.class.getName());

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
