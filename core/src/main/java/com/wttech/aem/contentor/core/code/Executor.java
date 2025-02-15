package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import com.wttech.aem.contentor.core.utils.OutputStreamWrapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.OutputStream;
import java.nio.file.Files;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = Executor.class)
public class Executor {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private BundleContext bundleContext;

    @Activate
    @Modified
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ExecutionContext createContext(Executable executable, ResourceResolver resourceResolver) {
        return new ExecutionContext(executable, bundleContext, resourceResolver);
    }

    public Execution execute(Executable executable) throws ContentorException {
        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            return execute(createContext(executable, resourceResolver));
        } catch (LoginException e) {
            throw new ContentorException(
                    String.format("Failed to access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(ExecutionContext context) throws ContentorException {
        try {
            ImmediateExecution execution = executeImmediately(context);
            if (context.isHistory() && context.getMode() == ExecutionMode.EVALUATE) {
                ExecutionHistory history = new ExecutionHistory(context.getResourceResolver());
                history.save(execution);
            }
            return execution;
        } finally {
            ExecutionOutput.delete(context.getId());
        }
    }

    private ImmediateExecution executeImmediately(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context);

        try (OutputStream outputStream =
                new OutputStreamWrapper(Files.newOutputStream(ExecutionOutput.path(context.getId())))) {
            context.setOutputStream(outputStream);

            GroovyShell shell = createShell(context);
            Script script = shell.parse(composeScript(context.getExecutable()), CodeSyntax.MAIN_CLASS);
            script.invokeMethod(CodeSyntax.Methods.INIT.givenName, null);

            execution.start();

            boolean runnable = (Boolean) script.invokeMethod(CodeSyntax.Methods.CHECK.givenName, null);
            if (!runnable) {
                return execution.end(ExecutionStatus.SKIPPED);
            }

            switch (context.getMode()) {
                case PARSE:
                    break;
                case EVALUATE:
                    script.invokeMethod(CodeSyntax.Methods.RUN.givenName, null);
                    break;
            }

            return execution.end(ExecutionStatus.SUCCEEDED);
        } catch (Throwable e) {
            execution.error(e);
            if ((e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                return execution.end(ExecutionStatus.ABORTED);
            }
            return execution.end(ExecutionStatus.FAILED);
        }
    }

    private String composeScript(Executable executable) throws ContentorException {
        StringBuilder builder = new StringBuilder();
        builder.append("void ").append(CodeSyntax.Methods.INIT.givenName).append("() {\n");
        builder.append("\tSystem.setOut(new java.io.PrintStream(")
                .append(Variable.OUT.varName())
                .append(", true, \"UTF-8\"));\n");
        builder.append("\tSystem.setErr(new java.io.PrintStream(")
                .append(Variable.OUT.varName())
                .append(", true, \"UTF-8\"));\n");
        builder.append("}\n");
        builder.append("\n");
        builder.append(executable.getContent());
        return builder.toString();
    }

    private GroovyShell createShell(ExecutionContext context) {
        Binding binding = new CodeBinding(context).toBinding();
        CompilerConfiguration compiler = new CompilerConfiguration();
        compiler.addCompilationCustomizers(new ImportCustomizer());
        compiler.addCompilationCustomizers(new ASTTransformationCustomizer(new CodeSyntax()));

        return new GroovyShell(binding, compiler);
    }
}
