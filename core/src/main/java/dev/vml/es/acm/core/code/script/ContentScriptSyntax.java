package dev.vml.es.acm.core.code.script;

import static dev.vml.es.acm.core.code.script.ScriptUtils.*;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class ContentScriptSyntax extends AbstractASTTransformation {

    public static final String MAIN_CLASS = "AcmContentScript";

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
        DESCRIBE("describeRun", "void", false),
        RUN("doRun", "void", true),
        CHECK("canRun", "boolean", true);

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
