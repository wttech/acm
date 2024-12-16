package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ExceptionUtils;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
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

import java.nio.charset.StandardCharsets;
import java.util.Date;

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
            return execute(executable, createContext(executable, resourceResolver));
        } catch (LoginException e) {
            throw new ContentorException(
                    String.format("Failed to access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(Executable executable, ExecutionContext context) throws ContentorException {
        Execution execution = executeImmediately(executable, context);
        if (context.isHistory() && context.getMode() == ExecutionMode.EVALUATE) {
            ExecutionHistory history = new ExecutionHistory(context.getResourceResolver());
            history.save(execution);
        }
        return execution;
    }

    private ImmediateExecution executeImmediately(Executable executable, ExecutionContext context) {
        String id = ExecutionId.generate();
        String content = composeContent(executable);

        StringBuilder simpleOutput = new StringBuilder();
        boolean simpleOutputActive = context.getOutputStream() == null;
        if (simpleOutputActive) {
            context.setOutputStream(new WriterOutputStream(new StringBuilderWriter(simpleOutput), StandardCharsets.UTF_8));
        }

        GroovyShell shell = createShell(context);
        Date startDate = new Date();

        try {
            Script script = shell.parse(content, CodeSyntax.MAIN_CLASS);
            script.invokeMethod(CodeSyntax.Methods.INIT.givenName, null);
            boolean runnable = (Boolean) script.invokeMethod(CodeSyntax.Methods.CHECK.givenName, null);
            if (!runnable) {
                return new ImmediateExecution(executable, id, ExecutionStatus.SKIPPED, startDate,
                        simpleOutputActive ? simpleOutput.toString() : null, null);
            }
            switch (context.getMode()) {
                case PARSE:
                    break;
                case EVALUATE:
                    script.invokeMethod(CodeSyntax.Methods.RUN.givenName, null);
                    break;
            }
            return new ImmediateExecution(executable, id, ExecutionStatus.SUCCEEDED, startDate,
                    simpleOutputActive ? simpleOutput.toString() : null, null);
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof InterruptedException) {
                return new ImmediateExecution(executable, id, ExecutionStatus.ABORTED, startDate,
                        simpleOutputActive ? simpleOutput.toString() : null, ExceptionUtils.toString(e));
            }
            return new ImmediateExecution(executable, id, ExecutionStatus.FAILED, startDate,
                    simpleOutputActive ? simpleOutput.toString() : null, ExceptionUtils.toString(e));
        }
    }

    private String composeContent(Executable executable) throws ContentorException {
        StringBuilder builder = new StringBuilder();
        builder.append("void ").append(CodeSyntax.Methods.INIT.givenName).append("() {\n");
        builder.append("\tSystem.setOut(new java.io.PrintStream(").append(Variable.OUT.varName()).append(", true, \"UTF-8\"));\n");
        builder.append("\tSystem.setErr(new java.io.PrintStream(").append(Variable.OUT.varName()).append(", true, \"UTF-8\"));\n");
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
