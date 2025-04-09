package com.wttech.aem.acm.core.code.script;

import com.wttech.aem.acm.core.code.AbstractCodeSyntax;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class ContentCodeSyntax extends AbstractCodeSyntax {

    public static final String MAIN_CLASS = "AcmContentCode";

    public static final Method DESCRIBE_RUN = new Method("describeRun", "void", false);

    public static final Method DO_RUN = new Method("doRun", "void", true);

    public static final Method CAN_RUN = new Method("canRun", "boolean", true);

    private static final Method[] METHODS = {DESCRIBE_RUN, DO_RUN, CAN_RUN};

    @Override
    protected String getMainClassName() {
        return MAIN_CLASS;
    }

    @Override
    protected Method[] getMethods() {
        return METHODS;
    }
}
