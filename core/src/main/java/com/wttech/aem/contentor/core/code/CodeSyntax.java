package com.wttech.aem.contentor.core.code;

import java.util.List;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class CodeSyntax extends AbstractASTTransformation {

    public static final String MAIN_CLASS = "ContentorCode";

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (source == null) {
            return;
        }

        this.sourceUnit = source;

        ClassNode mainClass = requireMainClass(source.getAST().getClasses());
        for (Methods methodValue : Methods.values()) {
            if (!hasMethod(mainClass, methodValue)) {
                addError(
                        String.format(
                                "Top-level '%s %s()' method not found!", methodValue.returnType, methodValue.givenName),
                        mainClass);
            }
        }
    }

    private ClassNode requireMainClass(List<ClassNode> classes) {
        ClassNode mainClass = null;
        for (ClassNode classNode : classes) {
            if (classNode != null && MAIN_CLASS.equals(classNode.getName())) {
                mainClass = classNode;
                break;
            }
        }

        if (mainClass == null) {
            throw new GroovyBugError(String.format(
                    "Class '%s' not found! Check file name provided to parse/evaluate method.", MAIN_CLASS));
        }
        return mainClass;
    }

    private boolean hasMethod(ClassNode mainClass, Methods methodValue) {
        for (MethodNode method : mainClass.getMethods()) {
            if (methodValue.givenName.equals(method.getName())
                    && methodValue.returnType.equals(method.getReturnType().getName())
                    && method.getParameters().length == 0) {
                return true;
            }
        }
        return false;
    }

    enum Methods {
        RUN("doRun", "void"),
        CHECK("canRun", "boolean");

        final String givenName;

        final String returnType;

        Methods(String givenName, String returnType) {
            this.givenName = givenName;
            this.returnType = returnType;
        }
    }
}
