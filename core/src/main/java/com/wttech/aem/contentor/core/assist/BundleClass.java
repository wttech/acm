package com.wttech.aem.contentor.core.assist;

import java.io.Serializable;

public class BundleClass implements Serializable {

    private final String clazz;

    private final String bundle;

    public BundleClass(String clazz, String bundle) {
        this.clazz = clazz;
        this.bundle = bundle;
    }

    public String getClazz() {
        return clazz;
    }

    public String getBundle() {
        return bundle;
    }
}
