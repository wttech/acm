package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.repo.LockInfo;
import dev.vml.es.acm.core.repo.Locker;
import dev.vml.es.acm.core.repo.RepoUtils;
import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

class ExecutionLock {

    static final long ATTEMPT_LIMIT = 2;

    private static final int RETRY_COUNT = 5;

    private static final String EXECUTION_ID_PROP = "executionId";

    private static final String ATTEMPT_PROP = "attempt";

    private static final String ALIVE_PROP = "alive";

    private static final String ALIVE_TIMEOUT_PROP = "aliveTimeout";

    enum State {
        FREE,
        OTHER_RUN,
        OWN_LEFT_BY_KILLED_RUN,
        OWN_RUN_STILL_ALIVE,
        OWN_ATTEMPT_LIMIT_REACHED
    }

    private final String path;

    private final String executionId;

    private long attempt;

    private long aliveWrittenNanoTime;

    private final long aliveTimeoutMillis;

    private volatile boolean takenOverByLaterAttempt;

    ExecutionLock(String lockName, String executionId, long aliveTimeoutMillis) {
        this.path = Locker.ROOT + "/" + lockName;
        this.executionId = executionId;
        this.aliveTimeoutMillis = aliveTimeoutMillis;
    }

    long getAliveIntervalMillis() {
        return aliveTimeoutMillis / 6;
    }

    /** The run stops itself before another delivery may take over. */
    long getAliveStopMillis() {
        return aliveTimeoutMillis / 3 * 2;
    }

    /** Stays true once seen, also after the later attempt released the lock. */
    boolean isTakenOverByLaterAttempt() {
        return takenOverByLaterAttempt;
    }

    long getAttempt() {
        return attempt;
    }

    long getAliveWrittenNanoTime() {
        return aliveWrittenNanoTime;
    }

    State read(ResourceResolver resolver) {
        Resource lock = resolver.getResource(path);
        if (!isActive(lock)) {
            return State.FREE;
        }
        ValueMap props = lock.getValueMap();
        if (!StringUtils.equals(executionId, props.get(EXECUTION_ID_PROP, String.class))) {
            return State.OTHER_RUN;
        }
        if (props.get(ATTEMPT_PROP, ATTEMPT_LIMIT) >= ATTEMPT_LIMIT) {
            return State.OWN_ATTEMPT_LIMIT_REACHED;
        }
        if (isAlive(props)) {
            return State.OWN_RUN_STILL_ALIVE;
        }
        return State.OWN_LEFT_BY_KILLED_RUN;
    }

    boolean create(ResourceResolver resolver, Duration ttl) {
        PersistenceException lastError = null;
        for (int retry = 0; retry < RETRY_COUNT; retry++) {
            try {
                return tryCreate(resolver, ttl);
            } catch (PersistenceException e) {
                discardChanges(resolver);
                lastError = e;
            }
        }
        throw new AcmException(String.format("Cannot create execution lock '%s'!", path), lastError);
    }

    private boolean tryCreate(ResourceResolver resolver, Duration ttl) throws PersistenceException {
        Resource current = resolver.getResource(path);
        if (isActive(current)) {
            return false;
        }
        deleteInactiveLock(resolver, current);
        long writeNanoTime = System.nanoTime();
        resolver.create(lockFolder(resolver), lockNodeName(), firstAttemptProperties(ttl));
        resolver.commit();
        holdAttempt(1, writeNanoTime);
        return true;
    }

    /** Reads the lock again, because it may have changed since enter() read it. */
    boolean reenter(ResourceResolver resolver, Duration ttl) {
        if (read(resolver) != State.OWN_LEFT_BY_KILLED_RUN) {
            return false;
        }
        long writeNanoTime = System.nanoTime();
        long nextAttempt = writeNextAttempt(resolver, ttl);
        try {
            resolver.commit();
        } catch (PersistenceException e) {
            resolver.revert();
            return false; // another delivery changed the lock
        }
        holdAttempt(nextAttempt, writeNanoTime);
        return true;
    }

    private long writeNextAttempt(ResourceResolver resolver, Duration ttl) {
        ModifiableValueMap props = resolver.getResource(path).adaptTo(ModifiableValueMap.class);
        long nextAttempt = props.get(ATTEMPT_PROP, ATTEMPT_LIMIT) + 1;
        props.put(ATTEMPT_PROP, nextAttempt);
        props.put(ALIVE_PROP, Calendar.getInstance());
        props.put(ALIVE_TIMEOUT_PROP, aliveTimeoutMillis);
        putLockedUntil(props, ttl);
        return nextAttempt;
    }

    boolean signalAlive(ResourceResolver resolver) {
        Resource lock = resolver.getResource(path);
        if (!isHeld(lock)) {
            return false;
        }
        lock.adaptTo(ModifiableValueMap.class).put(ALIVE_PROP, Calendar.getInstance());
        try {
            resolver.commit();
        } catch (PersistenceException e) {
            resolver.revert();
            throw new AcmException(String.format("Cannot refresh execution lock '%s'!", path), e);
        }
        return true;
    }

    boolean release(ResourceResolver resolver) {
        PersistenceException lastError = null;
        for (int retry = 0; retry < RETRY_COUNT; retry++) {
            Resource lock = resolver.getResource(path);
            if (!isHeld(lock)) {
                return false;
            }
            try {
                resolver.delete(lock);
                resolver.commit();
                return true;
            } catch (PersistenceException e) {
                resolver.revert();
                resolver.refresh();
                lastError = e;
            }
        }
        throw new AcmException(String.format("Cannot release execution lock '%s'!", path), lastError);
    }

    boolean isHeld(ResourceResolver resolver) {
        return isHeld(resolver.getResource(path));
    }

    private boolean isHeld(Resource lock) {
        if (lock == null) {
            return false;
        }
        noteTakeoverByLaterAttempt(lock.getValueMap());
        return isHeld(lock.getValueMap());
    }

    private void noteTakeoverByLaterAttempt(ValueMap props) {
        if (attempt > 0
                && StringUtils.equals(executionId, props.get(EXECUTION_ID_PROP, String.class))
                && props.get(ATTEMPT_PROP, 0L) > attempt) {
            takenOverByLaterAttempt = true;
        }
    }

    /** Called only after the lock write is committed. */
    private void holdAttempt(long heldAttempt, long writeNanoTime) {
        attempt = heldAttempt;
        aliveWrittenNanoTime = writeNanoTime;
    }

    private void deleteInactiveLock(ResourceResolver resolver, Resource lock) throws PersistenceException {
        if (lock != null) {
            resolver.delete(lock);
        }
    }

    private Resource lockFolder(ResourceResolver resolver) {
        return RepoUtils.ensure(
                resolver, StringUtils.substringBeforeLast(path, "/"), JcrResourceConstants.NT_SLING_FOLDER, true);
    }

    private String lockNodeName() {
        return StringUtils.substringAfterLast(path, "/");
    }

    private Map<String, Object> firstAttemptProperties(Duration ttl) {
        Calendar now = Calendar.getInstance();
        Map<String, Object> props = new HashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        props.put(Locker.LOCKED_PROP, now);
        putLockedUntil(props, ttl);
        props.put(EXECUTION_ID_PROP, executionId);
        props.put(ATTEMPT_PROP, 1L);
        props.put(ALIVE_PROP, now);
        props.put(ALIVE_TIMEOUT_PROP, aliveTimeoutMillis);
        return props;
    }

    private void discardChanges(ResourceResolver resolver) {
        resolver.revert();
        resolver.refresh();
    }

    /** Both attempts of a job have the same execution ID. The attempt number tells them apart. */
    private boolean isHeld(ValueMap props) {
        return attempt > 0
                && StringUtils.equals(executionId, props.get(EXECUTION_ID_PROP, String.class))
                && attempt == props.get(ATTEMPT_PROP, 0L);
    }

    /** Same rule as Locker. */
    private boolean isActive(Resource lock) {
        return lock != null && lock.getValueMap().containsKey(Locker.LOCKED_PROP) && !isExpired(lock.getValueMap());
    }

    private boolean isExpired(ValueMap props) {
        return new LockInfo(
                        props.get(Locker.LOCKED_PROP, Calendar.class),
                        props.get(Locker.LOCKED_UNTIL_PROP, Calendar.class))
                .isExpired();
    }

    /** Uses the timeout stored on the lock, so instances with different settings judge the same way. */
    private boolean isAlive(ValueMap props) {
        Calendar alive = props.get(ALIVE_PROP, Calendar.class);
        Long storedTimeout = props.get(ALIVE_TIMEOUT_PROP, Long.class);
        return alive == null
                || storedTimeout == null
                || System.currentTimeMillis() - alive.getTimeInMillis()
                        < Math.max(ShutdownSafety.MIN_RERUN_KILLED_EXECUTION_ALIVE_TIMEOUT, storedTimeout);
    }

    private void putLockedUntil(Map<String, Object> props, Duration ttl) {
        Calendar lockedUntil = Locker.lockedUntil(ttl);
        if (lockedUntil == null) {
            props.remove(Locker.LOCKED_UNTIL_PROP);
        } else {
            props.put(Locker.LOCKED_UNTIL_PROP, lockedUntil);
        }
    }
}
