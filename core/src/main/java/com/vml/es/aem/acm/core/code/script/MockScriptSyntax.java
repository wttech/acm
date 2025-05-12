package com.vml.es.aem.acm.core.code.script;

import static com.vml.es.aem.acm.core.code.script.ScriptUtils.*;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
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

        ClassNode mainClass = requireMainClass(source.getAST().getClasses(), MAIN_CLASS);
        for (Method methodValue : Method.values()) {
            if (methodValue.required || hasMethod(mainClass, methodValue.givenName)) {
                if (!isMethodValid(mainClass, methodValue.givenName, methodValue.returnType, 0)) {
                    addError(
                            String.format(
                                    "Top-level '%s %s()' method not found or has incorrect signature!",
                                    methodValue.returnType, methodValue.givenName),
                            mainClass);
                }
            }
        }
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
