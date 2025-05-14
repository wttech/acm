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

    private final MockType type;

    public MockScriptSyntax(MockType type) {
        this.type = type;
    }

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (source == null) {
            return;
        }

        this.sourceUnit = source;

        ClassNode mainClass = requireMainClass(source.getAST().getClasses(), MAIN_CLASS);
        for (Method methodValue : Method.values()) {
            if (isMethodRequired(methodValue) || hasMethod(mainClass, methodValue.givenName)) {
                if (!isMethodValid(mainClass, methodValue.givenName, methodValue.returnType, methodValue.paramCount)) {
                    addError(
                            String.format(
                                    "Top-level '%s %s()' method not found or has incorrect signature!",
                                    methodValue.returnType, methodValue.givenName),
                            mainClass);
                }
            }
        }
    }

    private boolean isMethodRequired(Method method) {
        switch (type) {
            case REGULAR:
                return method == Method.REQUEST || method == Method.RESPOND;
            case MISSING:
                return method == Method.RESPOND;
            case FAIL:
                return method == Method.FAIL;
            default:
                return false;
        }
    }

    enum Method {
        REQUEST("request", "boolean", 1),
        RESPOND("respond", "void", 2),
        FAIL("fail", "void", 3);

        final String givenName;

        final String returnType;

        final int paramCount;

        Method(String givenName, String returnType, int paramCount) {
            this.givenName = givenName;
            this.returnType = returnType;
            this.paramCount = paramCount;
        }
    }
}
