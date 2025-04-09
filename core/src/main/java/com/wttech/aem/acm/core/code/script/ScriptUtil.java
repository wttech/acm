package com.wttech.aem.acm.core.code.script;

import com.wttech.aem.acm.core.code.AbstractCodeSyntax;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

public final class ScriptUtil {

    private ScriptUtil() {
        // intentionally empty
    }

    public static GroovyShell createShell(AbstractCodeSyntax codeSyntax) {
        CompilerConfiguration compiler = new CompilerConfiguration();
        compiler.addCompilationCustomizers(new ImportCustomizer());
        compiler.addCompilationCustomizers(new ASTTransformationCustomizer(codeSyntax));
        return new GroovyShell(new Binding(), compiler);
    }

}
