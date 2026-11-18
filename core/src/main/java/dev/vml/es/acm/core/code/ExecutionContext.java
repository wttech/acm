package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.code.log.LogInterceptorManager;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.DurationUtils;
import dev.vml.es.acm.core.util.ResolverUtils;
import groovy.lang.Binding;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;

public class ExecutionContext implements AutoCloseable {

    public static final String VAR_ROOT = AcmConstants.VAR_ROOT + "/execution/context";

    public static String varPath(String executionId) {
        return String.format("%s/%s", VAR_ROOT, StringUtils.replace(executionId, "/", "-"));
    }

    private final String id;

    private final String userId;

    private final ExecutionMode mode;

    private final Executor executor;

    private final Executable executable;

    private final CodeContext codeContext;

    private final CodeOutput codeOutput;

    private final CodePrintStream printStream;

    private boolean history = true;

    private boolean notifications = true;

    private boolean debug = false;

    private boolean skipped = false;

    private Duration lockTimeout = null;

    private boolean abortOnShutdown = false;

    private final AtomicReference<AbortCause> abortCause = new AtomicReference<>();

    private volatile boolean lockLost;

    private volatile boolean lockConfirmationRequired;

    private volatile long lockConfirmedNanoTime;

    private volatile long lockStopMillis;

    private volatile boolean takenOverByLaterAttempt;

    private final Inputs inputs;

    private InputValues inputValues;

    private final Outputs outputs;

    private final Schedules schedules;

    private final Conditions conditions;

    private Consumer<ExecutionStatus> statusListener;

    public ExecutionContext(
            String id,
            String userId,
            ExecutionMode mode,
            Executor executor,
            Executable executable,
            InputValues inputValues,
            CodeContext codeContext,
            CodeOutput codeOutput) {
        this.id = id;
        this.userId = userId;
        this.mode = mode;
        this.executor = executor;
        this.executable = executable;
        this.inputValues = inputValues;
        this.codeContext = codeContext;
        this.codeOutput = codeOutput;
        this.printStream = new CodePrintStream(
                codeOutput.write(),
                String.format("%s|%s", executable.getId(), id),
                codeContext
                        .getOsgiContext()
                        .findService(LogInterceptorManager.class)
                        .orElse(null));
        this.schedules = new Schedules();
        this.conditions = new Conditions(this);
        this.inputs = new Inputs();
        this.outputs = new Outputs(this);

        customizeBinding();
    }

    private void cleanOutputs() {
        ResourceResolverFactory resolverFactory =
                codeContext.getOsgiContext().getService(ResourceResolverFactory.class);
        ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
            Repo.quiet(resolver).get(ExecutionContext.varPath(getId())).delete();
        });
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Executable getExecutable() {
        return executable;
    }

    public CodeContext getCodeContext() {
        return codeContext;
    }

    public CodeOutput getCodeOutput() {
        return codeOutput;
    }

    @Deprecated
    public CodeOutput getOutput() {
        return getCodeOutput();
    }

    public CodePrintStream getOut() {
        return getPrintStream();
    }

    public CodePrintStream getPrintStream() {
        return printStream;
    }

    public Logger getLogger() {
        return printStream.getLogger();
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public Duration getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(Object lockTimeout) {
        this.lockTimeout = DurationUtils.toDuration(lockTimeout);
    }

    boolean isAbortOnShutdown() {
        return abortOnShutdown;
    }

    void setAbortOnShutdown(boolean abortOnShutdown) {
        this.abortOnShutdown = abortOnShutdown;
    }

    public AbortCause getAbortCause() {
        return abortCause.get();
    }

    public boolean isAborted() {
        if (isStoppedByQueue()) {
            return true;
        }
        if (isLockLostOrUnconfirmed()) {
            stoppedForLock();
            return true;
        }
        return false;
    }

    private boolean isStoppedByQueue() {
        if (!abortOnShutdown) {
            return getCodeContext()
                    .getOsgiContext()
                    .getService(ExecutionQueue.class)
                    .isStoppingOrStopped(getId());
        }
        ExecutionQueue queue = findExecutionQueue();
        if (queue == null) {
            abortCause.set(AbortCause.INSTANCE_SHUTDOWN);
            return true;
        }
        return queue.isStoppingOrStopped(getId());
    }

    private ExecutionQueue findExecutionQueue() {
        try {
            return getCodeContext()
                    .getOsgiContext()
                    .findService(ExecutionQueue.class)
                    .orElse(null);
        } catch (NullPointerException | IllegalStateException e) {
            return null; // the bundle context is gone while ACM or the instance stops
        }
    }

    public void abort() {
        if (abortCause.get() == AbortCause.INSTANCE_SHUTDOWN) {
            throw new AbortException(AbortCause.INSTANCE_SHUTDOWN);
        }
        if (abortCause.get() == AbortCause.EXECUTION_LOCK_LOST || isLockLostOrUnconfirmed()) {
            stoppedForLock();
            throw new AbortException(abortCause.get());
        }
        throw new AbortException("Execution aborted gracefully!");
    }

    /** A later attempt of the same execution reports the result, so this run reports nothing. */
    void stopForLaterAttempt() {
        takenOverByLaterAttempt = true;
        stopForLostLock();
    }

    boolean isTakenOverByLaterAttempt() {
        return takenOverByLaterAttempt;
    }

    void requireLockConfirmation(long stopMillis, long confirmedNanoTime) {
        lockStopMillis = stopMillis;
        confirmLockHeldAt(confirmedNanoTime);
        lockConfirmationRequired = true;
    }

    void stopForLostLock() {
        lockLost = true;
        stoppedForLock();
    }

    /** A script told to stop ends aborted, even if it returns by itself. */
    private void stoppedForLock() {
        abortCause.compareAndSet(null, AbortCause.EXECUTION_LOCK_LOST); // a shutdown cause wins
    }

    /** Uses System.nanoTime(), so a change of the system clock does not stop the script. */
    void confirmLockHeldAt(long nanoTime) {
        lockConfirmedNanoTime = nanoTime;
    }

    private boolean isLockLostOrUnconfirmed() {
        return lockLost || isLockConfirmationTooOld();
    }

    private boolean isLockConfirmationTooOld() {
        return lockConfirmationRequired && millisSinceLockConfirmed() > lockStopMillis;
    }

    private long millisSinceLockConfirmed() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lockConfirmedNanoTime);
    }

    public void checkAborted() throws AbortException {
        if (isAborted()) {
            abort();
        }
    }

    public Inputs getInputs() {
        return inputs;
    }

    void useInputValues() {
        inputs.setValues(inputValues);
    }

    public Outputs getOutputs() {
        return outputs;
    }

    public Schedules getSchedules() {
        return schedules;
    }

    public Conditions getConditions() {
        return conditions;
    }

    private void customizeBinding() {
        codeContext
                .getRepo()
                .setLogger(printStream.bridgeLogger(codeContext.getRepo().getLogger()));
        codeContext
                .getAcl()
                .getContext()
                .setLogger(printStream.bridgeLogger(
                        codeContext.getAcl().getContext().getLogger()));

        Binding binding = getCodeContext().getBinding();

        binding.setVariable("context", this);
        binding.setVariable("schedules", schedules);
        binding.setVariable("arguments", inputs); // TODO deprecated
        binding.setVariable("inputs", inputs);
        binding.setVariable("outputs", outputs);
        binding.setVariable("conditions", conditions);
        binding.setVariable("out", getOut());
        binding.setVariable("log", getLogger());
    }

    @Override
    public void close() {
        printStream.close();
        codeOutput.close();
        outputs.close();
        cleanOutputs();
    }

    public void variable(String name, Object value) {
        codeContext.getBinding().setVariable(name, value);
    }

    public Object variable(String name) {
        return codeContext.getBinding().getVariable(name);
    }

    void listenStatus(Consumer<ExecutionStatus> statusListener) {
        this.statusListener = statusListener;
    }

    void notifyStatus(ExecutionStatus status) {
        if (statusListener != null) {
            statusListener.accept(status);
        }
    }
}
