package com.vml.es.aem.acm.core.code.script;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class MockScriptSyntax extends AbstractASTTransformation {

    public static final String MAIN_CLASS = "AcmMockScript";

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (source == null) {
            return;
        }
        this.sourceUnit = source;
        ScriptUtils.visit(this, nodes, source, MAIN_CLASS);
    }

    enum Method {
        REQUEST("request", "boolean", true),
        RESPOND("respond", "void", true),
        FAIL("fail", "void", false);

        final String givenName;

        final String returnType;

        final boolean required;

        Method(String givenName, String returnType, boolean required) {
            this.givenName = givenName;
            this.returnType = returnType;
            this.required = required;
        }
    }
}
