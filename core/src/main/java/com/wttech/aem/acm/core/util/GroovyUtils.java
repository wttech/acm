package com.wttech.aem.acm.core.util;

import groovy.lang.Closure;

public class GroovyUtils {

    public static <T> T with(T instance, Closure<T> closure) {
        if (closure != null) {
            closure.setDelegate(instance);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
        }
        return instance;
    }
}
