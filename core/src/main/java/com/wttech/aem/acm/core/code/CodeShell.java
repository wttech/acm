package com.wttech.aem.acm.core.code;

import groovy.lang.GroovyShell;

public class CodeShell {

    private final GroovyShell groovyShell;

    private final CodeBinding codeBinding;

    public CodeShell(GroovyShell groovyShell, CodeBinding codeBinding) {
        this.groovyShell = groovyShell;
        this.codeBinding = codeBinding;
    }

    public GroovyShell getGroovyShell() {
        return groovyShell;
    }

    public CodeBinding getCodeBinding() {
        return codeBinding;
    }
}
