package com.wttech.aem.acm.core.code;

import groovy.lang.GroovyShell;

public class Shell {

    private final GroovyShell groovyShell;

    private final Bindings bindings;

    public Shell(GroovyShell groovyShell, Bindings bindings) {
        this.groovyShell = groovyShell;
        this.bindings = bindings;
    }

    public GroovyShell getGroovyShell() {
        return groovyShell;
    }

    public Bindings getBindings() {
        return bindings;
    }
}
