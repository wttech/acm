package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.script.ContentScript;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = Executor.class)
@Designate(ocd = Executor.Config.class)
public class Executor {

    @ObjectClassDefinition(name = "AEM Content Manager - Code Executor")
    public @interface Config {

        @AttributeDefinition(name = "Keep history", description = "Save executions in history.")
        boolean history() default true;

        @AttributeDefinition(
                name = "Debug mode",
                description =
                        "Enables debug mode for troubleshooting. Changed behaviors include: start saving skipped executions in history.")
        boolean debug() default false;

        @AttributeDefinition(
                name = "Locking",
                description = "Prevents concurrent execution of the same executable. Enable this option and use 'condition.unlocked()' in your scripts. This is especially useful for scripts running on clustered author instances in AEMaaCS.")
        boolean locking() default true;
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private OsgiContext osgiContext;

    private Config config;

    private final Map<String, ExecutionStatus> statuses = new ConcurrentHashMap<>();

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public ExecutionContext createContext(
            String id, ExecutionMode mode, Executable executable, ResourceResolver resourceResolver) {
        CodeContext codeContext = new CodeContext(osgiContext, resourceResolver);
        ExecutionContext result = new ExecutionContext(id, mode, this, executable, codeContext);
        result.setDebug(config.debug());
        result.setHistory(config.history());
        return result;
    }

    public Execution execute(Executable executable, ExecutionContextOptions contextOptions) throws AcmException {
        try (ResourceResolver resourceResolver =
                        ResourceUtils.contentResolver(resourceResolverFactory, contextOptions.getUserId());
                ExecutionContext executionContext = createContext(
                        ExecutionId.generate(), contextOptions.getExecutionMode(), executable, resourceResolver)) {
            return execute(executionContext);
        } catch (LoginException e) {
            throw new AcmException(
                    String.format("Cannot access repository while executing '%s'", executable.getId()), e);
        }
    }

    public Execution execute(ExecutionContext context) throws AcmException {
        context.getCodeContext().prepareRun(context);
        ImmediateExecution execution = executeImmediately(context);
        if (context.getMode() == ExecutionMode.RUN) {
            if (context.isHistory() && (context.isDebug() || (execution.getStatus() != ExecutionStatus.SKIPPED))) {
                ExecutionHistory history =
                        new ExecutionHistory(context.getCodeContext().getResourceResolver());
                history.save(context, execution);
            }
            context.getCodeContext().completeRun(execution);
        }
        return execution;
    }

    private ImmediateExecution executeImmediately(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context);

        try {
            if (config.locking()) {
                context.getCodeContext().getLocker().lock(executableLockName(context));
            }
            statuses.put(context.getId(), ExecutionStatus.PARSING);

            ContentScript contentScript = new ContentScript(context);

            execution.start();

            if (context.getMode() == ExecutionMode.PARSE) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            }

            statuses.put(context.getId(), ExecutionStatus.CHECKING);

            contentScript.describe();
            context.getArguments().setValues(context.getExecutable().getArguments());

            boolean canRun = contentScript.canRun();
            if (!canRun) {
                return execution.end(ExecutionStatus.SKIPPED);
            } else if (context.getMode() == ExecutionMode.CHECK) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            }

            statuses.put(context.getId(), ExecutionStatus.RUNNING);
            contentScript.run();
            return execution.end(ExecutionStatus.SUCCEEDED);
        } catch (Throwable e) {
            execution.error(e);
            if ((e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                return execution.end(ExecutionStatus.ABORTED);
            }
            return execution.end(ExecutionStatus.FAILED);
        } finally {
            statuses.remove(context.getId());
            if (config.locking()) {
                context.getCodeContext().getLocker().unlock(executableLockName(context));
            }
        }
    }

    private String executableLockName(ExecutionContext context) {
        return context.getExecutable().getId();
    }

    public Optional<ExecutionStatus> checkStatus(String executionId) {
        return Optional.ofNullable(statuses.get(executionId));
    }

    public Description describe(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context);
        try {
            ContentScript contentScript = new ContentScript(context);
            execution.start();
            contentScript.describe();

            return new Description(execution.end(ExecutionStatus.SUCCEEDED), context.getArguments());
        } catch (Throwable e) {
            execution.error(e);
            return new Description(execution.end(ExecutionStatus.FAILED), new Arguments());
        }
    }
}
