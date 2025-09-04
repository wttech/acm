package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.script.ContentScript;
import dev.vml.es.acm.core.instance.InstanceSettings;
import dev.vml.es.acm.core.notification.NotificationManager;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
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

    public static final String LOCK_DIR = "executor";

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
                name = "Log Printing Enabled",
                description =
                        "Prints logs in the execution output. Disable if strict control over the script output is needed.")
        boolean logPrintingEnabled() default true;

        @AttributeDefinition(
                name = "Log Printing Names",
                description = "Additional loggers to print logs from (class names or package names)")
        String[] logPrintingNames() default {CodeLoggerPrinter.NAME_ACL, CodeLoggerPrinter.NAME_REPO};

        @AttributeDefinition(name = "Notification Enabled", description = "Enables notifications for completed executions.")
        boolean notificationEnabled() default true;

        @AttributeDefinition(
                name = "Notification Executable IDs",
                description = "Allow to control with regular expressions which executables should be notified about.")
        String[] notificationExecutableIds() default {"/conf/acm/script/automatic/.*"};
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private OsgiContext osgiContext;

    @Reference
    private NotificationManager notifierManager;

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
                        ResolverUtils.contentResolver(resourceResolverFactory, contextOptions.getUserId());
                ExecutionContext executionContext = createContext(
                        ExecutionId.generate(), contextOptions.getExecutionMode(), executable, resourceResolver)) {
            return execute(executionContext);
        } catch (LoginException e) {
            throw new AcmException(
                    String.format("Cannot access repository while executing '%s'!", executable.getId()), e);
        }
    }

    public Execution execute(ExecutionContext context) throws AcmException {
        context.getCodeContext().prepareRun(context);
        ImmediateExecution execution = executeImmediately(context);
        if (context.getMode() == ExecutionMode.RUN) {
            handleHistory(context, execution);
            handleNotifications(context, execution);
            context.getCodeContext().completeRun(execution);
        }
        return execution;
    }

    private ImmediateExecution executeImmediately(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context).start();

        try {
            statuses.put(context.getId(), ExecutionStatus.PARSING);

            ContentScript contentScript = new ContentScript(context);

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

            Locker locker = context.getCodeContext().getLocker();
            String lockName = executableLockName(context);

            if (locker.isLocked(lockName)) {
                return execution.end(ExecutionStatus.SKIPPED);
            }

            try {
                locker.lock(lockName);
                statuses.put(context.getId(), ExecutionStatus.RUNNING);
                if (config.logPrintingEnabled()) {
                    context.getOut().fromSelfLogger();
                    context.getOut().fromLoggers(config.logPrintingNames());
                }
                contentScript.run();
                return execution.end(ExecutionStatus.SUCCEEDED);
            } finally {
                locker.unlock(lockName);
            }
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

    private String executableLockName(ExecutionContext context) {
        return String.format(
                "%s/%s",
                LOCK_DIR, StringUtils.removeStart(context.getExecutable().getId(), AcmConstants.SETTINGS_ROOT + "/"));
    }

    public Optional<ExecutionStatus> checkStatus(String executionId) {
        return Optional.ofNullable(statuses.get(executionId));
    }

    private void handleHistory(ExecutionContext context, ImmediateExecution execution) {
        if (context.isHistory() && (context.isDebug() || (execution.getStatus() != ExecutionStatus.SKIPPED))) {
            ExecutionHistory history =
                    new ExecutionHistory(context.getCodeContext().getResourceResolver());
            history.save(context, execution);
        }
    }

    private void handleNotifications(ExecutionContext context, ImmediateExecution execution) {
        String executableId = execution.getExecutable().getId();
        if (!config.notificationEnabled()
                || !notifierManager.isConfigured()
                || !Arrays.stream(config.notificationExecutableIds())
                        .anyMatch(regex -> Pattern.matches(regex, executableId))) {
            return;
        }

        InstanceInfo instanceInfo = context.getCodeContext().getOsgiContext().getInstanceInfo();
        InstanceSettings instanceSettings = new InstanceSettings(instanceInfo);
        String statusIcon = execution.getStatus() == ExecutionStatus.SUCCEEDED
                ? "✅"
                : (execution.getStatus() == ExecutionStatus.FAILED ? "❌" : "⚠️");
        String instanceRoleName = instanceSettings.getRole().name().toLowerCase();
        String title = statusIcon + " " + AcmConstants.NAME;
        String text = String.format("Execution completed: %s", executableId);

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("Status", execution.getStatus().name().toLowerCase());
        fields.put("Time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        fields.put("Duration", execution.getDuration() + "ms");
        fields.put("Instance", instanceSettings.getId() + " (" + instanceRoleName + ")");

        notifierManager.sendMessage(title, text, fields);
    }

    public Description describe(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context).start();
        try {
            ContentScript contentScript = new ContentScript(context);
            contentScript.describe();

            return new Description(execution.end(ExecutionStatus.SUCCEEDED), context.getArguments());
        } catch (Throwable e) {
            execution.error(e);
            return new Description(execution.end(ExecutionStatus.FAILED), new Arguments());
        }
    }

    public Execution check(ExecutionContext context) throws AcmException {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context).start();

        try {
            ContentScript contentScript = new ContentScript(context);
            boolean canRun = contentScript.canRun();
            if (canRun) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            } else {
                return execution.end(ExecutionStatus.SKIPPED);
            }
        } catch (Throwable e) {
            execution.error(e);
            return execution.end(ExecutionStatus.FAILED);
        }
    }

    public ScheduleResult schedule(ExecutionContext context) {
        ImmediateExecution.Builder execution = new ImmediateExecution.Builder(context).start();
        try {
            ContentScript contentScript = new ContentScript(context);
            Schedule schedule = contentScript.schedule();
            return new ScheduleResult(execution.end(ExecutionStatus.SUCCEEDED), schedule);
        } catch (Throwable e) {
            execution.error(e);
            return new ScheduleResult(execution.end(ExecutionStatus.FAILED), null);
        }
    }

    public boolean isDebug() {
        return config.debug();
    }

    public boolean isHistory() {
        return config.history();
    }

    public boolean isLocked(ExecutionContext context) {
        return context.getCodeContext().getLocker().isLocked(executableLockName(context));
    }
}
