package com.wttech.aem.acm.core.code;

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.List;

// TODO extract abstract for both syntaxes
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class ExtensionCodeSyntax extends AbstractASTTransformation {

    public static final String MAIN_CLASS = "AcmExtensionCode";

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (source == null) {
            return;
        }

        this.sourceUnit = source;

        ClassNode mainClass = requireMainClass(source.getAST().getClasses());
        for (Method methodValue : Method.values()) {
            if (methodValue.required || hasMethod(mainClass, methodValue)) {
                if (!isMethodValid(mainClass, methodValue)) {
                    addError(
                            String.format(
                                    "Top-level '%s %s()' method not found or has incorrect signature!",
                                    methodValue.returnType, methodValue.givenName),
                            mainClass);
                }
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

    private boolean hasMethod(ClassNode mainClass, Method methodValue) {
        for (MethodNode method : mainClass.getMethods()) {
            if (methodValue.givenName.equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isMethodValid(ClassNode mainClass, Method methodValue) {
        for (MethodNode method : mainClass.getMethods()) {
            if (methodValue.givenName.equals(method.getName())
                    && methodValue.returnType.equals(method.getReturnType().getName())
                    && method.getParameters().length == 0) {
                return true;
            }
        }
        return false;
    }

    enum Method {
        EXTEND("extendRun", "void", true),
        COMPLETE("completeRun", "void", true);

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
