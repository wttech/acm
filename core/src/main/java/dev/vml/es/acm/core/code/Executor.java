package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.script.ContentScript;
import dev.vml.es.acm.core.event.Event;
import dev.vml.es.acm.core.event.EventListener;
import dev.vml.es.acm.core.event.EventType;
import dev.vml.es.acm.core.format.TemplateFormatter;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.InstanceSettings;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.notification.NotificationManager;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.repo.Locker;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptType;
import dev.vml.es.acm.core.state.Permissions;
import dev.vml.es.acm.core.util.DateUtils;
import dev.vml.es.acm.core.util.DurationUtils;
import dev.vml.es.acm.core.util.ResolverUtils;
import dev.vml.es.acm.core.util.StringUtil;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {Executor.class, EventListener.class})
@Designate(ocd = Executor.Config.class)
@SuppressWarnings("java:S1181")
public class Executor implements EventListener {

    public static final String LOCK_DIR = "executor";

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

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
        String[] logPrintingNames() default {CodePrintStream.LOGGER_NAME_ACL, CodePrintStream.LOGGER_NAME_REPO};

        @AttributeDefinition(
                name = "Log Printing Timestamps",
                description = "Prints timestamps in the execution output.")
        boolean logPrintingTimestamps() default true;

        @AttributeDefinition(
                name = "Notification Enabled",
                description = "Enables notifications for completed executions.")
        boolean notificationEnabled() default true;

        @AttributeDefinition(name = "Notification Notifier ID")
        String notificationNotifierId() default AcmConstants.NOTIFIER_ID;

        @AttributeDefinition(
                name = "Notification Executable IDs",
                description = "Allow to control with regular expressions which executables should be notified about.")
        String[] notificationExecutableIds() default {"/conf/acm/settings/script/automatic/.*"};

        @AttributeDefinition(
                name = "Notification Title",
                description = "Template variables: context, execution, statusIcon, statusHere")
        String notificationTitle() default "${statusIcon} ACM Code Execution";

        @AttributeDefinition(
                name = "Notification Text",
                description = "Template variables: context, execution, statusIcon, statusHere")
        String notificationText() default "Completed: ${execution.executable.id} ${statusHere}";

        @AttributeDefinition(
                name = "Notification Details Length",
                description = "Max length of the output and error. Use negative value to skip abbreviation.")
        int notificationDetailsLength() default 512;

        @AttributeDefinition(
                name = "Lock Timeout",
                description =
                        "Expiration time (in milliseconds) of executable locks, protecting against stale locks left behind when an instance is killed abruptly (e.g. pod recycling). Must exceed the max expected execution time. Use a zero or negative value to disable expiration.")
        long lockTimeout() default 24L * 60L * 60L * 1000L;
    }

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private OsgiContext osgiContext;

    @Reference
    private NotificationManager notifier;

    @Reference
    private SpaSettings spaSettings;

    @Reference
    private ShutdownSafety shutdownSafety;

    /** Optional: without it no lock is ever taken over from an instance that left the cluster. */
    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile DiscoveryService discoveryService;

    /** Optional: without it no lock is ever taken over from an instance that left the cluster. */
    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile JobManager jobManager;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = EventType.of(event.getName()).orElse(null);
        if (eventType == EventType.EXECUTOR_RESET) {
            reset();
        }
    }

    public boolean authorize(Executable executable, String userId) {
        return ResolverUtils.queryContentResolver(resolverFactory, userId, resolver -> {
            return authorize(executable, resolver);
        });
    }

    public boolean authorize(Executable executable, ResourceResolver resolver) {
        return isFeatureEnabled(executable, resolver) && isExecutableAvailable(executable, resolver);
    }

    /** Stored scripts run with their repository content, so script rights never allow running other code. */
    public Optional<Code> authorizeCode(Code requested, ResourceResolver resolver) {
        if (requested == null || requested.getId() == null) {
            return Optional.empty();
        }
        Optional<Code> code = Executable.CONSOLE_ID.equals(requested.getId())
                ? Optional.of(requested)
                : new ScriptRepository(resolver)
                        .read(requested.getId())
                        .map(script -> new Code(script.getId(), script.getContent()));
        return code.filter(c -> authorize(c, resolver));
    }

    public boolean authorizeNoHistory(ResourceResolver resolver) {
        return Permissions.check(Permissions.Feature.CONSOLE_EXECUTE_NOHISTORY, resolver);
    }

    private boolean isFeatureEnabled(Executable executable, ResourceResolver resolver) {
        if (Executable.CONSOLE_ID.equals(executable.getId())) {
            return Permissions.check(Permissions.Feature.CONSOLE_EXECUTE, resolver);
        }
        return Permissions.check(Permissions.Feature.SCRIPT_EXECUTE, resolver);
    }

    private boolean isExecutableAvailable(Executable executable, ResourceResolver resolver) {
        String scriptPath = executable.getId();
        if (Executable.CONSOLE_ID.equals(executable.getId())) {
            scriptPath = Executable.CONSOLE_SCRIPT_PATH;
        }
        ScriptRepository repository = new ScriptRepository(resolver);
        return repository.read(scriptPath).isPresent();
    }

    public ExecutionContext createContext(
            String id,
            String userId,
            ExecutionMode mode,
            Executable executable,
            InputValues inputs,
            ResourceResolver resourceResolver,
            CodeOutput codeOutput) {
        CodeContext codeContext = new CodeContext(osgiContext, resourceResolver);
        ExecutionContext result =
                new ExecutionContext(id, userId, mode, this, executable, inputs, codeContext, codeOutput);
        result.setDebug(config.debug());
        result.setHistory(config.history());
        return result;
    }

    public Execution execute(Executable executable, ExecutionContextOptions contextOptions) throws AcmException {
        try (ResourceResolver resolver = ResolverUtils.contentResolver(resolverFactory, contextOptions.getUserId());
                ExecutionContext executionContext = createContext(
                        ExecutionId.generate(),
                        contextOptions.getUserId(),
                        contextOptions.getExecutionMode(),
                        executable,
                        contextOptions.getInputs(),
                        resolver,
                        determineOutput(contextOptions.getExecutionMode(), ExecutionId.generate()))) {
            return execute(executionContext);
        } catch (LoginException e) {
            throw new AcmException(
                    String.format("Cannot access repository while executing '%s'!", executable.getId()), e);
        }
    }

    private CodeOutput determineOutput(ExecutionMode mode, String executionId) {
        return mode == ExecutionMode.RUN
                ? new CodeOutputRepo(resolverFactory, spaSettings, executionId)
                : new CodeOutputMemory();
    }

    public Execution execute(ExecutionContext context) throws AcmException {
        context.getCodeContext().prepareRun(context);
        RunLock runLock = createRunLock(context);
        ContextualExecution execution = executeInternal(context, runLock);
        if (context.getMode() == ExecutionMode.RUN) {
            if (runLock.isTakenOverByLaterAttempt()) {
                LOG.warn(
                        "Execution '{}' was taken over by its later attempt, which reports the result",
                        context.getId());
                // the decision is read from the repository, which scripts can write, so leave a trace
                ExecutionAudit.info("Code run not reported, its later attempt reports the result", context, execution);
            } else {
                if (!runLock.isResultSavedBefore()) {
                    handleHistory(execution);
                }
                handleNotifications(execution);
                context.getCodeContext().completeRun(execution);
            }
        }
        if (runLock.isLeftBehind()) {
            // a succeeded Sling job is deleted, and a later boot takes over the lock only when the job ended otherwise
            LOG.warn(
                    "Execution lock of '{}' is left behind by '{}', reporting the job as failed",
                    context.getExecutable().getId(),
                    context.getId());
            throw new AcmException(String.format(
                    "Execution lock of '%s' is left behind by '%s'",
                    context.getExecutable().getId(), context.getId()));
        }
        return execution;
    }

    private ContextualExecution executeInternal(ExecutionContext context, RunLock runLock) {
        ContextualExecution.Builder execution = new ContextualExecution.Builder(context).start();

        boolean healthChecking = isHealthChecking(context);
        if ((!healthChecking && context.isSkipped())) {
            return execution.end(ExecutionStatus.SKIPPED);
        }

        try {
            context.notifyStatus(ExecutionStatus.PARSING);

            ContentScript contentScript = new ContentScript(context);

            if (context.getMode() == ExecutionMode.PARSE) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            }

            context.notifyStatus(ExecutionStatus.CHECKING);

            contentScript.describe();
            context.useInputValues();

            boolean canRun = contentScript.canRun();
            if (!canRun) {
                return execution.end(ExecutionStatus.SKIPPED);
            } else if (context.getMode() == ExecutionMode.CHECK) {
                return execution.end(ExecutionStatus.SUCCEEDED);
            }

            if (!runLock.enter()) {
                return execution.end(ExecutionStatus.LOCKED);
            }

            try {
                runLock.hold();
                context.notifyStatus(ExecutionStatus.RUNNING);
                if (!healthChecking && config.logPrintingEnabled()) {
                    context.getOut().fromSelfLogger();
                    context.getOut().fromLoggers(config.logPrintingNames());
                    context.getOut().setLoggerTimestamps(config.logPrintingTimestamps());
                }
                contentScript.run();

                if (context.isLockLost() || !runLock.isHeld()) {
                    return abortedByLostLock(context, execution, null);
                }
                if (!healthChecking) {
                    LOG.info("Execution succeeded '{}'", context.getId());
                }
                return execution.end(ExecutionStatus.SUCCEEDED);
            } finally {
                runLock.release();
            }
        } catch (AbortException e) {
            LOG.warn("Execution aborted gracefully '{}'", context.getId());
            execution.error(e);
            return execution.end(ExecutionStatus.ABORTED);
        } catch (Throwable e) {
            if (context.isLockLost()) {
                return abortedByLostLock(context, execution, e);
            }
            if ((e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                LOG.warn("Execution aborted forcefully '{}'", context.getId());
                execution.error(e);
                return execution.end(ExecutionStatus.ABORTED);
            } else {
                if (context.getMode() == ExecutionMode.PARSE) {
                    // Compile errors go back to the caller; editors and agents check code on every change.
                    LOG.debug("Execution parsing failed '{}': {}", context.getId(), e.getMessage());
                } else if (!healthChecking) {
                    LOG.error("Execution failed '{}'", context.getId(), e);
                }
                execution.error(e);
                return execution.end(ExecutionStatus.FAILED);
            }
        }
    }

    private boolean isHealthChecking(ExecutionContext context) {
        return ExecutionId.HEALTH_CHECK.equals(context.getId());
    }

    /** Only creates the lock; the run enters it after its checks. */
    private RunLock createRunLock(ExecutionContext context) {
        if (isHealthChecking(context)) {
            return RunLock.NONE;
        }
        String lockName = executableLockName(context);
        Supplier<Duration> ttl = () -> lockTimeout(context);
        return takesOverDeadLock(context)
                ? new OwnedRunLock(resolverFactory, context, ownedExecutionLock(context), ttl)
                : new LockerRunLock(resolverFactory, context, lockName, ttl);
    }

    /** Only automatic scripts can run again after a kill. */
    private boolean takesOverDeadLock(ExecutionContext context) {
        return shutdownSafety.isEnabled()
                && ScriptType.byPath(context.getExecutable().getId()).orElse(null) == ScriptType.AUTOMATIC;
    }

    private ContextualExecution abortedByLostLock(
            ExecutionContext context, ContextualExecution.Builder execution, Throwable error) {
        context.stopForLostLock();
        LOG.warn("{} '{}'", ExecutionContext.LOCK_LOST_MESSAGE, context.getId());
        execution.error(new AbortException(ExecutionContext.LOCK_LOST_MESSAGE, error));
        return execution.end(ExecutionStatus.ABORTED);
    }

    private String executableLockName(ExecutionContext context) {
        return String.format(
                "%s/%s",
                LOCK_DIR, StringUtils.removeStart(context.getExecutable().getId(), AcmConstants.SETTINGS_ROOT + "/"));
    }

    private Duration lockTimeout(ExecutionContext context) {
        if (context.getLockTimeout() != null) {
            return context.getLockTimeout();
        }
        return DurationUtils.toDuration(config.lockTimeout());
    }

    private void handleHistory(ContextualExecution execution) {
        if (execution.getContext().isHistory()
                && (execution.getContext().isDebug()
                        || !ExecutionStatus.notRun().contains(execution.getStatus()))) {
            useHistory(resolverFactory, h -> h.save(execution));
        }
    }

    private void handleNotifications(ContextualExecution execution) {
        String executableId = execution.getExecutable().getId();
        if (!execution.getContext().isNotifications()
                || !config.notificationEnabled()
                || !notifier.isConfigured(config.notificationNotifierId())
                || Arrays.stream(config.notificationExecutableIds())
                        .noneMatch(regex -> Pattern.matches(regex, executableId))) {
            return;
        }

        Map<String, Object> templateVars = new LinkedHashMap<>();
        templateVars.put("context", execution.getContext());
        templateVars.put("execution", execution);
        templateVars.put(
                "statusIcon",
                execution.getStatus() == ExecutionStatus.SUCCEEDED
                        ? "✅"
                        : (execution.getStatus() == ExecutionStatus.FAILED ? "❌" : "⚠️"));
        templateVars.put("statusHere", execution.getStatus() == ExecutionStatus.SUCCEEDED ? "" : "@here");
        String title = StringUtils.trim(formatTemplate(config.notificationTitle(), templateVars));
        String text = StringUtils.trim(formatTemplate(config.notificationText(), templateVars));

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("Status", execution.getStatus().name().toLowerCase());
        fields.put("Time", DateUtils.humanFormat().format(new Date()));
        fields.put("Duration", StringUtil.formatDuration(execution.getDuration()));

        InstanceInfo instanceInfo =
                execution.getContext().getCodeContext().getOsgiContext().getInstanceInfo();
        InstanceSettings instanceSettings = new InstanceSettings(instanceInfo);
        String instanceRoleName = instanceSettings.getRole().name().toLowerCase();
        String instanceId = instanceSettings.getId();
        String instanceDesc = instanceId.toLowerCase().contains(instanceRoleName)
                ? instanceId
                : instanceId + " (" + instanceRoleName + ")";
        fields.put("Instance", instanceDesc);
        fields.put("Output", StringUtil.truncateCodeStart(execution.getOutput(), config.notificationDetailsLength()));
        fields.put("Error", StringUtil.truncateCodeEnd(execution.getError(), config.notificationDetailsLength()));

        notifier.sendMessageTo(config.notificationNotifierId(), title, text, fields);
    }

    public Description describe(ExecutionContext context) {
        ContextualExecution.Builder execution = new ContextualExecution.Builder(context).start();
        try {
            ContentScript contentScript = new ContentScript(context);
            contentScript.describe();

            return new Description(execution.end(ExecutionStatus.SUCCEEDED), context.getInputs());
        } catch (Throwable e) {
            execution.error(e);
            return new Description(execution.end(ExecutionStatus.FAILED), new Inputs());
        }
    }

    public Execution check(ExecutionContext context) throws AcmException {
        ContextualExecution.Builder execution = new ContextualExecution.Builder(context).start();

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
        ContextualExecution.Builder execution = new ContextualExecution.Builder(context).start();
        try {
            ContentScript contentScript = new ContentScript(context);
            Schedule schedule = contentScript.schedule();
            return new ScheduleResult(execution.end(ExecutionStatus.SUCCEEDED), schedule);
        } catch (Throwable e) {
            execution.error(e);
            return new ScheduleResult(execution.end(ExecutionStatus.FAILED), null);
        }
    }

    public void reset() {
        useLocker(resolverFactory, l -> l.unlockAll());
    }

    public boolean isDebug() {
        return config.debug();
    }

    public boolean isHistory() {
        return config.history();
    }

    public boolean isLocked(ExecutionContext context) {
        if (takesOverDeadLock(context) && isLockLeftByGoneInstance(context)) {
            return false;
        }
        return queryLocker(resolverFactory, l -> l.isLocked(executableLockName(context)));
    }

    /** The run takes such a lock over, so a boot must not skip the script because of it. */
    private boolean isLockLeftByGoneInstance(ExecutionContext context) {
        ExecutionLock lock = ownedExecutionLock(context);
        return ResolverUtils.queryContentResolver(resolverFactory, null, lock::read)
                == ExecutionLock.State.LEFT_BY_GONE_INSTANCE;
    }

    private ExecutionLock ownedExecutionLock(ExecutionContext context) {
        return new ExecutionLock(
                executableLockName(context),
                context.getId(),
                shutdownSafety.getLockAliveTimeout(),
                shutdownSafety.getLockAttemptLimit(),
                new LockOwnerPresence(discoveryService),
                new LockOwnerJob(jobManager));
    }

    private <T> T queryLocker(ResourceResolverFactory resolverFactory, Function<Locker, T> consumer) {
        return ResolverUtils.queryContentResolver(resolverFactory, null, r -> consumer.apply(new Locker(r)));
    }

    private void useLocker(ResourceResolverFactory resolverFactory, Consumer<Locker> consumer) {
        ResolverUtils.useContentResolver(resolverFactory, null, r -> consumer.accept(new Locker(r)));
    }

    private void useHistory(ResourceResolverFactory resolverFactory, Consumer<ExecutionHistory> consumer) {
        ResolverUtils.useContentResolver(resolverFactory, null, r -> consumer.accept(new ExecutionHistory(r)));
    }

    private String formatTemplate(String template, Map<String, Object> vars) {
        try {
            return new TemplateFormatter().renderString(template, vars);
        } catch (Exception e) {
            LOG.warn("Cannot format template '{}'!", template, e);
            return template;
        }
    }
}
