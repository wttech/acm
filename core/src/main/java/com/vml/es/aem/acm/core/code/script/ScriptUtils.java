package com.vml.es.aem.acm.core.code.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.util.List;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.AbstractASTTransformation;

public final class ScriptUtils {

    private ScriptUtils() {
        // intentionally empty
    }

    public static void visit(
            AbstractASTTransformation transformation, ASTNode[] nodes, SourceUnit source, String mainClassName) {
        ClassNode mainClass = requireMainClass(source.getAST().getClasses(), mainClassName);
        for (MockScriptSyntax.Method methodValue : MockScriptSyntax.Method.values()) {
            if (methodValue.required || hasMethod(mainClass, methodValue.givenName)) {
                if (!isMethodValid(mainClass, methodValue.givenName, methodValue.returnType, 0)) {
                    transformation.addError(
                            String.format(
                                    "Top-level '%s %s()' method not found or has incorrect signature!",
                                    methodValue.returnType, methodValue.givenName),
                            mainClass);
                }
            }
        }
    }

    public static GroovyShell createShell(ASTTransformation codeSyntax) {
        CompilerConfiguration compiler = new CompilerConfiguration();
        compiler.addCompilationCustomizers(new ImportCustomizer());
        compiler.addCompilationCustomizers(new ASTTransformationCustomizer(codeSyntax));
        return new GroovyShell(new Binding(), compiler);
    }

    public static boolean hasMethod(ClassNode mainClass, String methodName) {
        for (MethodNode method : mainClass.getMethods()) {
            if (methodName.equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMethodValid(ClassNode mainClass, String methodName, String returnType, int paramCount) {
        for (MethodNode method : mainClass.getMethods()) {
            if (methodName.equals(method.getName())
                    && returnType.equals(method.getReturnType().getName())
                    && method.getParameters().length == paramCount) {
                return true;
            }
        }
        return false;
    }

    public static ClassNode requireMainClass(List<ClassNode> classes, String mainClassName) {
        ClassNode mainClass = null;
        for (ClassNode classNode : classes) {
            if (classNode != null && mainClassName.equals(classNode.getName())) {
                mainClass = classNode;
                break;
            }
        }

        if (mainClass == null) {
            throw new GroovyBugError(String.format(
                    "Class '%s' not found! Check file name provided to parse/evaluate method.", mainClassName));
        }
        return mainClass;
    }
}
