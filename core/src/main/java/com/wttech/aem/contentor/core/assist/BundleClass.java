package com.wttech.aem.contentor.core.assist;

import java.io.Serializable;

public class BundleClass implements Serializable {

    private final String className;

    private final String bundleSymbolicName;

    public BundleClass(String className, String bundleSymbolicName) {
        this.className = className;
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getClassName() {
        return className;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }
}
