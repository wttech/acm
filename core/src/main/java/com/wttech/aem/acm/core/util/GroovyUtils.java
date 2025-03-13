package com.wttech.aem.acm.core.util;

import groovy.lang.Closure;

public class GroovyUtils {

    public static <T> T with(T instance, Closure<T> closure) {
        closure.setDelegate(instance);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST); // TODO check if could be skipped
        closure.call();
        return instance;
    }
}
