package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.repo.LockInfo;
import dev.vml.es.acm.core.repo.Locker;
import dev.vml.es.acm.core.util.DateUtils;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.time.Duration;
import java.util.Calendar;
import java.util.function.Supplier;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LockerRunLock implements RunLock {

    private static final Logger LOG = LoggerFactory.getLogger(LockerRunLock.class);

    private final ResourceResolverFactory resolverFactory;

    private final ExecutionContext context;

    private final String lockName;

    /** Read when the lock is written: a script sets its lock timeout in describeRun(). */
    private final Supplier<Duration> ttl;

    LockerRunLock(
            ResourceResolverFactory resolverFactory,
            ExecutionContext context,
            String lockName,
            Supplier<Duration> ttl) {
        this.resolverFactory = resolverFactory;
        this.context = context;
        this.lockName = lockName;
        this.ttl = ttl;
    }

    @Override
    public boolean enter() {
        LockInfo lockInfo =
                ResolverUtils.queryContentResolver(resolverFactory, null, r -> new Locker(r).readLock(lockName));
        if (lockInfo != null && !lockInfo.isExpired()) {
            LOG.warn(
                    "Execution locked '{}' by another run of '{}' since {} (expires {})",
                    context.getId(),
                    context.getExecutable().getId(),
                    formatLockTime(lockInfo.getLockedAt()),
                    formatLockTime(lockInfo.getLockedUntil()));
            return false;
        }
        return true;
    }

    @Override
    public void hold() {
        ResolverUtils.useContentResolver(resolverFactory, null, r -> new Locker(r).lock(lockName, ttl.get()));
    }

    /** A lock by name has no owner to check. */
    @Override
    public boolean isHeld() {
        return true;
    }

    @Override
    public void release() {
        ResolverUtils.useContentResolver(resolverFactory, null, r -> new Locker(r).unlock(lockName));
    }

    private String formatLockTime(Calendar calendar) {
        return calendar != null ? DateUtils.humanFormat().format(calendar.getTime()) : "never";
    }
}
