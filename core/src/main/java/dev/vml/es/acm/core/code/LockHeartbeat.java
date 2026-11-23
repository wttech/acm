package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.util.ResolverUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LockHeartbeat implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LockHeartbeat.class);

    private final ResourceResolverFactory resolverFactory;

    private final ExecutionLock lock;

    private final ExecutionContext context;

    private boolean lockLost;

    private volatile boolean stopped;

    private boolean failing;

    LockHeartbeat(ResourceResolverFactory resolverFactory, ExecutionLock lock, ExecutionContext context) {
        this.resolverFactory = resolverFactory;
        this.lock = lock;
        this.context = context;
    }

    private void stopForLostLock() {
        if (lock.isTakenOverByLaterAttempt()) {
            LOG.warn("Execution '{}' was taken over by its later attempt. Stopping it.", context.getId());
        } else {
            LOG.warn("Execution '{}' lost its execution lock to another run. Stopping it.", context.getId());
        }
        context.stopForLostLock();
    }

    /** The run releases its lock next, so a beat still running must not report the lock as lost. */
    void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        if (lockLost) {
            return;
        }
        try {
            boolean held = ResolverUtils.queryContentResolver(resolverFactory, null, lock::signalAlive);
            failing = false;
            if (stopped) {
                return;
            }
            if (!held) {
                lockLost = true;
                stopForLostLock();
            }
        } catch (Throwable e) { // an uncaught error would cancel all later beats
            if (!failing) {
                LOG.warn("Execution lock of '{}' cannot be kept alive: {}", context.getId(), e.toString());
            }
            failing = true;
        }
    }
}
