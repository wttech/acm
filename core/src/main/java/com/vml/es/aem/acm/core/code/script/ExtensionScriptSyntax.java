package com.vml.es.aem.acm.core.code.script;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class ExtensionScriptSyntax extends AbstractASTTransformation {

    public static final String MAIN_CLASS = "AcmExtensionScript";

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (source == null) {
            return;
        }
        this.sourceUnit = source;
        ScriptUtils.visit(this, nodes, source, MAIN_CLASS);
    }

    enum Method {
        PREPARE("prepareRun", "void", true, 1),
        COMPLETE("completeRun", "void", true, 1);

        final String givenName;
        final String returnType;
        final boolean required;
        final int paramCount;

        Method(String givenName, String returnType, boolean required, int paramCount) {
            this.givenName = givenName;
            this.returnType = returnType;
            this.required = required;
            this.paramCount = paramCount;
        }
    }
}
