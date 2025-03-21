package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.util.ResourceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.OutputStream;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = Executor.class)
@Designate(ocd = Executor.Config.class)
public class Executor {

    @ObjectClassDefinition(name = "AEM Content Manager - Executor")
    public @interface Config {

        @AttributeDefinition(name = "Keep history", description = "Save executions in history.")
        boolean history() default true;

        @AttributeDefinition(
                name = "Debug mode",
                description =
                        "Enables debug mode for troubleshooting. Changed behaviors include: start saving skipped executions in history.")
        boolean debug() default false;
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private OsgiContext osgiContext;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public ExecutionContext createContext(Executable executable, ResourceResolver resourceResolver) {
        ExecutionContext result = new ExecutionContext(executable, osgiContext, resourceResolver);
        result.setDebug(config.debug());
        result.setHistory(config.history());
        return result;
    }

    public Execution execute(Executable executable) throws AcmException {
        try (ResourceResolver resourceResolver = ResourceUtils.serviceResolver(resourceResolverFactory)) {
            return execute(createContext(executable, resourceResolver));
        } catch (LoginException e) {
            throw new AcmException(
                    String.format("Failed to access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(ExecutionContext context) throws AcmException {
        try {
            ImmediateExecution execution = executeImmediately(context);
            if (context.isHistory()
                    && (context.getMode() == ExecutionMode.EVALUATE)
                    && (context.isDebug() || (execution.getStatus() != ExecutionStatus.SKIPPED))) {
                ExecutionHistory history = new ExecutionHistory(context.getResourceResolver());
                history.save(execution);
            }
            return execution;
        } finally {
            context.getFileOutput().delete();
        }
    }

    private ImmediateExecution executeImmediately(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context);

        try (OutputStream outputStream = context.getFileOutput().write()) {
            if (context.getOutputStream() != null) {
                context.setOutputStream(new TeeOutputStream(outputStream, context.getOutputStream()));
            } else {
                context.setOutputStream(outputStream);
            }

            context.setOutputStream(outputStream);
            GroovyShell shell = createShell(context);

            execution.start();

            Script script = shell.parse(context.getExecutable().getContent(), CodeSyntax.MAIN_CLASS);

            switch (context.getMode()) {
                case PARSE:
                    break;
                case EVALUATE:
                    boolean runnable = (Boolean) script.invokeMethod(CodeSyntax.Methods.CHECK.givenName, null);
                    if (!runnable) {
                        return execution.end(ExecutionStatus.SKIPPED);
                    }
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

    private GroovyShell createShell(ExecutionContext context) {
        Binding binding = new CodeBinding(context).toBinding();
        CompilerConfiguration compiler = new CompilerConfiguration();
        compiler.addCompilationCustomizers(new ImportCustomizer());
        compiler.addCompilationCustomizers(new ASTTransformationCustomizer(new CodeSyntax()));

        return new GroovyShell(binding, compiler);
    }
}
