package com.wttech.aem.contentor.core.osgi;

import org.osgi.framework.Bundle;

public class ClassInfo {

    private final String className;

    private final Bundle bundle;

    public ClassInfo(String className, Bundle bundle) {
        this.className = className;
        this.bundle = bundle;
    }

    public String getClassName() {
        return className;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
