package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Locker {

    public static final String ROOT = AcmConstants.VAR_ROOT + "/lock";

    private static final Logger LOG = LoggerFactory.getLogger(Locker.class);

    private static final String RESOURCE_TYPE = JcrConstants.NT_UNSTRUCTURED;

    private static final String LOCKED_PROP = "locked";

    private static final String LOCKED_UNTIL_PROP = "lockedUntil";

    private static final int RETRY_COUNT = 5;

    private final ResourceResolver resolver;

    private final Supplier<Boolean> autoCommit;

    public Locker(ResourceResolver resolver) {
        this(resolver, () -> true);
    }

    public Locker(ResourceResolver resolver, Supplier<Boolean> autoCommit) {
        this.resolver = resolver;
        this.autoCommit = autoCommit;
    }

    public boolean isLocked(String lockName) {
        String name = normalizeName(lockName);
        Resource lock = getLock(name);
        return isLock(lock);
    }

    public LockInfo readLock(String lockName) {
        String name = normalizeName(lockName);
        Resource lock = getLock(name);
        if (!isLock(lock)) {
            return null;
        }
        return new LockInfo(
                lock.getValueMap().get(LOCKED_PROP, Calendar.class),
                lock.getValueMap().get(LOCKED_UNTIL_PROP, Calendar.class));
    }

    public void lock(String lockName) {
        lock(lockName, null);
    }

    public void lock(String lockName, Duration ttl) {
        String name = normalizeName(lockName);

        PersistenceException exceptionLast = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                Resource lockCurrent = getLock(name);
                if (lockCurrent != null) {
                    if (isExpired(lockCurrent)) {
                        LOG.debug("Recreating expired lock '{}'", name);
                        resolver.delete(lockCurrent);
                    } else {
                        LOG.warn("Cannot create lock '{}' as it already exists!", name);
                        return;
                    }
                }

                Resource dirResource;
                String nodeName;
                if (name.contains("/")) {
                    String dirPath = StringUtils.substringBeforeLast(name, "/");
                    nodeName = StringUtils.substringAfterLast(name, "/");
                    dirResource = getOrCreateDir(ROOT + "/" + dirPath);
                } else {
                    dirResource = getOrCreateDir(ROOT);
                    nodeName = name;
                }
                Map<String, Object> props = new HashMap<>();
                props.put(JcrConstants.JCR_PRIMARYTYPE, RESOURCE_TYPE);
                props.put(LOCKED_PROP, Calendar.getInstance());
                if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
                    Calendar lockedUntil = Calendar.getInstance();
                    lockedUntil.setTimeInMillis(lockedUntil.getTimeInMillis() + ttl.toMillis());
                    props.put(LOCKED_UNTIL_PROP, lockedUntil);
                }
                resolver.create(dirResource, nodeName, props);
                if (autoCommit.get()) {
                    resolver.commit();
                }
                LOG.debug("Created lock '{}'", name);
                return;
            } catch (PersistenceException e) {
                if (autoCommit.get()) {
                    resolver.revert();
                    resolver.refresh();
                    exceptionLast = e;
                    LOG.debug("Cannot create lock '{}' - retrying {}/{}", name, i + 1, RETRY_COUNT, e);
                } else {
                    throw new AcmException(String.format("Cannot create lock '%s'!", name), e);
                }
            }
        }
        throw new AcmException(
                String.format("Cannot create lock '%s' after %d retries!", name, RETRY_COUNT), exceptionLast);
    }

    private Resource getOrCreateDir(String path) throws PersistenceException {
        return RepoUtils.ensure(resolver, path, JcrResourceConstants.NT_SLING_FOLDER, true);
    }

    public void unlock(String lockName) {
        String name = normalizeName(lockName);

        PersistenceException exceptionLast = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                Resource lockCurrent = getLock(name);
                if (lockCurrent == null) {
                    LOG.warn("Cannot delete lock '{}' as it does not exist!", name);
                    return;
                }
                resolver.delete(lockCurrent);
                if (autoCommit.get()) {
                    resolver.commit();
                }
                LOG.debug("Deleted lock '{}'", name);
                return;
            } catch (PersistenceException e) {
                if (autoCommit.get()) {
                    resolver.revert();
                    resolver.refresh();
                    exceptionLast = e;
                    LOG.debug("Cannot delete lock '{}' - retrying {}/{}", name, i + 1, RETRY_COUNT, e);
                } else {
                    throw new AcmException(String.format("Cannot delete lock '%s'!", name), e);
                }
            }
        }
        throw new AcmException(
                String.format("Cannot delete lock '%s' after %d retries!", name, RETRY_COUNT), exceptionLast);
    }

    public void unlockAll() {
        Resource root = resolver.getResource(ROOT);
        if (root == null) {
            return;
        }
        try {
            resolver.delete(root);
            if (autoCommit.get()) {
                resolver.commit();
            }
            LOG.debug("Deleted all locks");
        } catch (PersistenceException e) {
            throw new AcmException("Cannot delete all locks!", e);
        }
    }

    private Resource getLock(String lockName) {
        String name = normalizeName(lockName);
        if (StringUtils.isBlank(name)) {
            throw new AcmException("Lock name cannot be blank!");
        }
        return resolver.getResource(ROOT + "/" + name);
    }

    private String normalizeName(String lockName) {
        return StringUtils.removeStart(lockName, AcmConstants.SETTINGS_ROOT + "/");
    }

    private Stream<Resource> locks() {
        Resource resource = resolver.getResource(ROOT);
        if (resource == null) {
            return Stream.empty();
        }
        return ResourceSpliterator.stream(resource).skip(1).filter(this::isLock);
    }

    private boolean isLock(Resource lock) {
        return lock != null
                && lock.isResourceType(RESOURCE_TYPE)
                && lock.getValueMap().containsKey(LOCKED_PROP)
                && !isExpired(lock);
    }

    private boolean isExpired(Resource lock) {
        Calendar lockedUntil = lock.getValueMap().get(LOCKED_UNTIL_PROP, Calendar.class);
        return lockedUntil != null && Calendar.getInstance().after(lockedUntil);
    }

    public boolean anyLocked() {
        return locks().findAny().isPresent();
    }

    public static class LockInfo {

        private final Calendar lockedAt;

        private final Calendar lockedUntil;

        public LockInfo(Calendar lockedAt, Calendar lockedUntil) {
            this.lockedAt = lockedAt;
            this.lockedUntil = lockedUntil;
        }

        public Calendar getLockedAt() {
            return lockedAt;
        }

        public Calendar getLockedUntil() {
            return lockedUntil;
        }
    }
}
