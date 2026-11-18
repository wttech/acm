package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OwnedRunLock implements RunLock {

    private static final Logger LOG = LoggerFactory.getLogger(OwnedRunLock.class);

    private static final long HEARTBEAT_STOP_TIMEOUT_SECONDS = 5;

    private final ResourceResolverFactory resolverFactory;

    private final ExecutionContext context;

    private final ExecutionLock lock;

    private final Duration ttl;

    private ScheduledExecutorService heartbeat;

    OwnedRunLock(
            ResourceResolverFactory resolverFactory,
            ExecutionContext context,
            String lockName,
            Duration ttl,
            long aliveTimeoutMillis) {
        this.resolverFactory = resolverFactory;
        this.context = context;
        this.lock = new ExecutionLock(lockName, context.getId(), aliveTimeoutMillis);
        this.ttl = ttl;
    }

    @Override
    public boolean enter() {
        ExecutionLock.State state = ResolverUtils.queryContentResolver(resolverFactory, null, lock::read);
        switch (state) {
            case FREE:
                return createLock();
            case OTHER_RUN:
                logLockedByAnotherRun();
                return false;
            case OWN_LEFT_BY_KILLED_RUN:
                return runAgainUnderOwnLock();
            case OWN_RUN_STILL_ALIVE:
                logLockedByOwnEarlierRun("earlier run still alive");
                return false;
            case OWN_ATTEMPT_LIMIT_REACHED:
            default:
                logLockedByOwnEarlierRun(String.format(
                        "attempt limit reached (%d of %d)", ExecutionLock.ATTEMPT_LIMIT, ExecutionLock.ATTEMPT_LIMIT));
                return false;
        }
    }

    @Override
    public boolean isHeld() {
        boolean held = ResolverUtils.queryContentResolver(resolverFactory, null, lock::isHeld);
        stopForLaterAttemptIfTakenOver();
        return held;
    }

    @Override
    public void hold() {
        context.requireLockConfirmation(lock.getAliveStopMillis(), lock.getAliveWrittenNanoTime());
        heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ACM execution lock heartbeat " + context.getId());
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleWithFixedDelay(
                new LockHeartbeat(resolverFactory, lock, context),
                lock.getAliveIntervalMillis(),
                lock.getAliveIntervalMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void release() {
        stopHeartbeat();
        boolean released = ResolverUtils.queryContentResolver(resolverFactory, null, lock::release);
        stopForLaterAttemptIfTakenOver();
        if (!released) {
            LOG.warn(
                    "Execution lock of '{}' not released by '{}': it is gone or held by another run",
                    context.getExecutable().getId(),
                    context.getId());
        }
    }

    private void stopForLaterAttemptIfTakenOver() {
        if (lock.isTakenOverByLaterAttempt()) {
            context.stopForLaterAttempt();
        }
    }

    private boolean createLock() {
        boolean created = ResolverUtils.queryContentResolver(resolverFactory, null, r -> lock.create(r, ttl));
        if (!created) {
            logLockedByAnotherRun();
        }
        return created;
    }

    private boolean runAgainUnderOwnLock() {
        if (isResultOfEarlierRunSaved()) {
            logLockedByOwnEarlierRun("earlier run already saved a result");
            return false;
        }
        boolean reentered = ResolverUtils.queryContentResolver(resolverFactory, null, r -> lock.reenter(r, ttl));
        if (!reentered) {
            LOG.warn(
                    "Execution locked '{}': its lock of '{}' changed while it tried to run again",
                    context.getId(),
                    context.getExecutable().getId());
            return false;
        }
        LOG.warn(
                "Execution '{}' runs again under its own lock of '{}' left by a killed run (attempt {} of {})",
                context.getId(),
                context.getExecutable().getId(),
                lock.getAttempt(),
                ExecutionLock.ATTEMPT_LIMIT);
        deleteOutputOfKilledRun();
        return true;
    }

    private boolean isResultOfEarlierRunSaved() {
        return ResolverUtils.queryContentResolver(
                resolverFactory, null, r -> new ExecutionHistory(r).contains(context.getId()));
    }

    /** A failed delete must not stop the run. Old output only looks untidy. */
    private void deleteOutputOfKilledRun() {
        try {
            ResolverUtils.useContentResolver(resolverFactory, null, r -> Repo.quiet(r)
                    .get(ExecutionContext.varPath(context.getId()))
                    .delete());
        } catch (RuntimeException e) {
            LOG.warn("Output of the killed run of '{}' cannot be deleted: {}", context.getId(), e.toString());
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

    private void stopHeartbeat() {
        if (heartbeat == null) {
            return;
        }
        heartbeat.shutdown();
        try {
            heartbeat.awaitTermination(HEARTBEAT_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
