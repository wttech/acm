package dev.vml.es.acm.core.code.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.transform.ASTTransformation;

public final class ScriptUtils {

    public static final String MIME_TYPE = "text/x-groovy";

    private static final String DEF_TYPE = "java.lang.Object";

    private ScriptUtils() {
        // intentionally empty
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
            if (methodName.equals(method.getName()) && method.getParameters().length == paramCount) {
                String actualReturnType = method.getReturnType().getName();
                if (returnType.equals(actualReturnType) || DEF_TYPE.equals(actualReturnType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ClassNode requireMainClass(List<ClassNode> classes, String mainClassName) {
        ClassNode mainClass = null;
        for (ClassNode classNode : classes) {
            if (classNode != null
                    && (StringUtils.equals(classNode.getName(), mainClassName)
                            || StringUtils.endsWith(classNode.getName(), "." + mainClassName))) {
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
