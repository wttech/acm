package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoUtils;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
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

    private final ResourceResolver resolver;

    public Locker(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isLocked(String lockName) {
        String name = normalizeName(lockName);
        Resource lock = getLock(name);
        return isLock(lock);
    }

    public void lock(String lockName) {
        String name = normalizeName(lockName);
        Resource lock = getLock(name);
        if (lock != null) {
            LOG.warn("Cannot create lock '{}' as it already exists!", name);
            return;
        }
        try {
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
            resolver.create(dirResource, nodeName, props);
            resolver.commit();
            LOG.debug("Created lock '{}'", name);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create lock '%s'!", name), e);
        }
    }

    private Resource getOrCreateDir(String path) throws PersistenceException {
        return RepoUtils.ensure(resolver, path, JcrResourceConstants.NT_SLING_FOLDER, true);
    }

    public void unlock(String lockName) {
        String name = normalizeName(lockName);
        Resource lock = getLock(name);
        if (lock == null) {
            LOG.warn("Cannot delete lock '{}' as it does not exist!", name);
            return;
        }
        try {
            resolver.delete(lock);
            resolver.commit();
            LOG.debug("Deleted lock '{}'", name);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot delete lock '%s'!", name), e);
        }
    }

    public void unlockAll() {
        Resource root = resolver.getResource(ROOT);
        if (root == null) {
            return;
        }
        try {
            resolver.delete(root);
            resolver.commit();
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
                && lock.getValueMap().containsKey(LOCKED_PROP);
    }

    public boolean anyLocked() {
        return locks().findAny().isPresent();
    }

    public boolean isLockStale(String lockName, long timeoutMillis) {
        Calendar lockTime = getLockTime(lockName);
        if (lockTime == null) {
            return true; // non-existent as stale
        }
        
        Calendar now = Calendar.getInstance();
        long diff = (now.getTimeInMillis() - lockTime.getTimeInMillis());
        
        boolean isStale = diff > timeoutMillis;
        if (isStale) {
            LOG.debug("Lock '{}' is {} millis old, considering it stale (timeout: {} millis)", lockName, diff, timeoutMillis);
        }
        return isStale;
    }

    private Calendar getLockTime(String lockName) {
        Resource lock = getLock(lockName);
        if (lock == null) {
            return null;
        }
        return lock.getValueMap().get(LOCKED_PROP, Calendar.class);
    }
}
