package com.wttech.aem.acm.core.code;

import groovy.lang.GroovyShell;

public class Shell {

    private final GroovyShell groovyShell;

    public Shell(GroovyShell groovyShell) {
        this.groovyShell = groovyShell;
    }

    public GroovyShell getGroovyShell() {
        return groovyShell;
    }

    // TODO register variable here in groovy context ; but also save metadata / documentation here / to be used in assistancer
}
