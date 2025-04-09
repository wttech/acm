package com.wttech.aem.acm.core.code;

import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class ExtensionCodeSyntax extends AbstractCodeSyntax {

    public static final String MAIN_CLASS = "AcmExtensionCode";

    public static final Method EXTEND_RUN = new Method("extendRun", "void", true);

    public static final Method COMPLETE_RUN = new Method("completeRun", "void", true);

    private static final Method[] METHODS = {EXTEND_RUN, COMPLETE_RUN};

    @Override
    protected String getMainClassName() {
        return MAIN_CLASS;
    }

    @Override
    protected Method[] getMethods() {
        return METHODS;
    }
}
