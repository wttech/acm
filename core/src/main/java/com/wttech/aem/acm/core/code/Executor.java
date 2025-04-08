package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.osgi.OsgiContext;
import com.wttech.aem.acm.core.util.ResourceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private Extender extender;

    @Reference
    private OsgiContext osgiContext;

    private Config config;

    private final Map<String, ExecutionStatus> statuses = new ConcurrentHashMap<>();

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public ExecutionContext createContext(Executable executable, ResourceResolver resourceResolver) {
        ExecutionContext result = new ExecutionContext(this, executable, osgiContext, resourceResolver);
        result.setDebug(config.debug());
        result.setHistory(config.history());
        return result;
    }

    public Execution execute(Executable executable, ExecutionContextOptions contextOptions) throws AcmException {
        try (ResourceResolver resourceResolver =
                ResourceUtils.serviceResolver(resourceResolverFactory, contextOptions.getUserId())) {
            return execute(createContext(executable, resourceResolver));
        } catch (LoginException e) {
            throw new AcmException(
                    String.format("Cannot access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(ExecutionContext context) throws AcmException {
        try {
            ImmediateExecution execution = executeImmediately(context);
            if (context.isHistory()
                    && (context.getMode() == ExecutionMode.RUN)
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
            statuses.put(context.getId(), ExecutionStatus.PARSING);

            if (context.getOutputStream() != null) {
                context.setOutputStream(new TeeOutputStream(outputStream, context.getOutputStream()));
            } else {
                context.setOutputStream(outputStream);
            }

            CodeShell shell = createShell(context);

            execution.start();

            Script script = shell.getGroovyShell().parse(context.getExecutable().getContent(), ContentCodeSyntax.MAIN_CLASS);
            if (context.getMode() == ExecutionMode.PARSE) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            }

            statuses.put(context.getId(), ExecutionStatus.CHECKING);

            try {
                script.invokeMethod(ContentCodeSyntax.Method.DESCRIBE.givenName, null);
                shell.getCodeBinding()
                        .getArguments()
                        .setValues(context.getExecutable().getArguments());
            } catch (MissingMethodException e) {
                // ignore
            }

            boolean canRun = (Boolean) script.invokeMethod(ContentCodeSyntax.Method.CHECK.givenName, null);
            if (!canRun) {
                return execution.end(ExecutionStatus.SKIPPED);
            } else if (context.getMode() == ExecutionMode.CHECK) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            }

            statuses.put(context.getId(), ExecutionStatus.RUNNING);
            script.invokeMethod(ContentCodeSyntax.Method.RUN.givenName, null);
            return execution.end(ExecutionStatus.SUCCEEDED);
        } catch (Throwable e) {
            execution.error(e);
            if ((e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                return execution.end(ExecutionStatus.ABORTED);
            }
            return execution.end(ExecutionStatus.FAILED);
        } finally {
            statuses.remove(context.getId());
        }
    }

    private CodeShell createShell(ExecutionContext context) {
        CodeBinding codeBinding = new CodeBinding(context);
        Binding binding = codeBinding.toBinding();
        CompilerConfiguration compiler = new CompilerConfiguration();
        compiler.addCompilationCustomizers(new ImportCustomizer());
        compiler.addCompilationCustomizers(new ASTTransformationCustomizer(new ContentCodeSyntax()));
        GroovyShell groovyShell = new GroovyShell(binding, compiler);
        return new CodeShell(groovyShell, codeBinding);
    }

    public Optional<ExecutionStatus> checkStatus(String executionId) {
        return Optional.ofNullable(statuses.get(executionId));
    }

    public Description describe(Executable executable, ResourceResolver resourceResolver) {
        return describe(createContext(executable, resourceResolver));
    }

    public Description describe(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context);

        try (OutputStream outputStream = context.getFileOutput().write()) {
            context.setOutputStream(outputStream);

            CodeShell shell = createShell(context);
            for (Script script : extender.getScripts()) {
                script.invokeMethod(ExtensionCodeSyntax.Method.EXTEND.givenName, null);
            }

            execution.start();
            Script script = shell.getGroovyShell().parse(context.getExecutable().getContent(), ContentCodeSyntax.MAIN_CLASS);

            try {
                script.invokeMethod(ContentCodeSyntax.Method.DESCRIBE.givenName, null);
            } catch (MissingMethodException e) {
                // ignore
            }
            return new Description(
                    execution.end(ExecutionStatus.SUCCEEDED),
                    shell.getCodeBinding().getArguments());
        } catch (Throwable e) {
            execution.error(e);
            return new Description(execution.end(ExecutionStatus.FAILED), new Arguments(context));
        } finally {
            context.getFileOutput().delete();
        }
    }
}
