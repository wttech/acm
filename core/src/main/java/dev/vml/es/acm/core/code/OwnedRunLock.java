package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OwnedRunLock implements RunLock {

    private static final Logger LOG = LoggerFactory.getLogger(OwnedRunLock.class);

    private final ResourceResolverFactory resolverFactory;

    private final ExecutionContext context;

    private final ExecutionLock lock;

    /** Read when the lock is written: a script sets its lock timeout in describeRun(). */
    private final Supplier<Duration> ttl;

    private ScheduledExecutorService heartbeat;

    private LockHeartbeat heartbeatTask;

    private boolean entered;

    private boolean resultSavedBefore;

    private boolean leftBehind;

    OwnedRunLock(
            ResourceResolverFactory resolverFactory,
            ExecutionContext context,
            ExecutionLock lock,
            Supplier<Duration> ttl) {
        this.resolverFactory = resolverFactory;
        this.context = context;
        this.lock = lock;
        this.ttl = ttl;
    }

    @Override
    public boolean enter() {
        entered = enterByState();
        return entered;
    }

    private boolean enterByState() {
        ExecutionLock.State state = ResolverUtils.queryContentResolver(resolverFactory, null, lock::read);
        switch (state) {
            case FREE:
                return createLock();
            case OTHER_RUN:
                logLockedByAnotherRun();
                return false;
            case LEFT_BY_GONE_INSTANCE:
                return takeOverLockOfGoneInstance();
            case OWN_LEFT_BY_DEAD_RUN:
                return runAgainUnderOwnLock();
            case OWN_RUN_STILL_ALIVE:
                logLockedByOwnEarlierRun("earlier run still alive");
                return false;
            case OWN_ATTEMPT_LIMIT_REACHED:
            default:
                logLockedByOwnEarlierRun(String.format("attempt limit reached (limit %d)", lock.getAttemptLimit()));
                return false;
        }
    }

    @Override
    public boolean isHeld() {
        return ResolverUtils.queryContentResolver(resolverFactory, null, lock::isHeld);
    }

    @Override
    public void hold() {
        heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ACM execution lock heartbeat " + context.getId());
            thread.setDaemon(true);
            return thread;
        });
        heartbeatTask = new LockHeartbeat(resolverFactory, lock, context);
        heartbeat.scheduleWithFixedDelay(
                heartbeatTask, lock.getAliveIntervalMillis(), lock.getAliveIntervalMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void release() {
        stopHeartbeat();
        boolean released;
        try {
            released = ResolverUtils.queryContentResolver(resolverFactory, null, lock::release);
        } catch (Throwable e) { // an error must not let the Sling job end as succeeded
            leftBehind = true;
            throw e;
        }
        if (!released) {
            LOG.warn(
                    "Execution lock of '{}' not released by '{}': it is gone or held by another run",
                    context.getExecutable().getId(),
                    context.getId());
        }
    }

    /** A later attempt may have saved its result and released the lock before this run noticed. */
    @Override
    public boolean isTakenOverByLaterAttempt() {
        return entered && (lock.isTakenOverByLaterAttempt() || isResultSaved());
    }

    @Override
    public boolean isLeftBehind() {
        return leftBehind;
    }

    @Override
    public boolean isResultSavedBefore() {
        return resultSavedBefore;
    }

    private boolean createLock() {
        if (refuseWhenEarlierRunSavedResult()) {
            return false;
        }
        boolean created = ResolverUtils.queryContentResolver(resolverFactory, null, r -> lock.create(r, ttl.get()));
        if (!created) {
            logLockedByAnotherRun();
        }
        return created;
    }

    private boolean runAgainUnderOwnLock() {
        if (refuseWhenEarlierRunSavedResult()) {
            return false;
        }
        boolean reentered = ResolverUtils.queryContentResolver(resolverFactory, null, r -> lock.reenter(r, ttl.get()));
        if (!reentered) {
            LOG.warn(
                    "Execution locked '{}': its lock of '{}' changed while it tried to run again",
                    context.getId(),
                    context.getExecutable().getId());
            return false;
        }
        LOG.warn(
                "Execution '{}' runs again under its own lock of '{}' left by a dead run (attempt {} of {})",
                context.getId(),
                context.getExecutable().getId(),
                lock.getAttempt(),
                lock.getAttemptLimit());
        deleteOutputOfDeadRun();
        return true;
    }

    private boolean takeOverLockOfGoneInstance() {
        boolean taken = ResolverUtils.queryContentResolver(resolverFactory, null, r -> lock.takeOver(r, ttl.get()));
        if (!taken) {
            logLockedByAnotherRun();
            return false;
        }
        LOG.warn(
                "Execution '{}' takes over the lock of '{}' left by an instance that left the cluster (attempt {} of {})",
                context.getId(),
                context.getExecutable().getId(),
                lock.getAttempt(),
                lock.getAttemptLimit());
        return true;
    }

    private boolean refuseWhenEarlierRunSavedResult() {
        if (!isResultSaved()) {
            return false;
        }
        resultSavedBefore = true;
        logLockedByOwnEarlierRun("earlier run already saved a result");
        return true;
    }

    private boolean isResultSaved() {
        return ResolverUtils.queryContentResolver(
                resolverFactory, null, r -> new ExecutionHistory(r).contains(context.getId()));
    }

    /** A failed delete must not stop the run. Old output only looks untidy. */
    private void deleteOutputOfDeadRun() {
        try {
            ResolverUtils.useContentResolver(resolverFactory, null, r -> Repo.quiet(r)
                    .get(ExecutionContext.varPath(context.getId()))
                    .delete());
        } catch (RuntimeException e) {
            LOG.warn("Output of the dead run of '{}' cannot be deleted: {}", context.getId(), e.toString());
        }
    }

    private void logLockedByOwnEarlierRun(String reason) {
        LOG.warn(
                "Execution locked '{}' by its own earlier run of '{}': {}",
                context.getId(),
                context.getExecutable().getId(),
                reason);
    }

    private void logLockedByAnotherRun() {
        LOG.warn(
                "Execution locked '{}' by another run of '{}'",
                context.getId(),
                context.getExecutable().getId());
    }

    /** Waits at most one beat interval; a slower beat reports nothing after the stop. */
    private void stopHeartbeat() {
        if (heartbeat == null) {
            return;
        }
        heartbeatTask.stop();
        heartbeat.shutdown();
        try {
            heartbeat.awaitTermination(lock.getAliveIntervalMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
