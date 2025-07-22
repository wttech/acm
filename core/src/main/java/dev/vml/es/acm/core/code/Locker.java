package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Locker {

    public static final String ROOT = AcmConstants.VAR_ROOT + "/lock";

    private static final Logger LOG = LoggerFactory.getLogger(Locker.class);

    private static final String RESOURCE_TYPE = JcrConstants.NT_UNSTRUCTURED;

    private final ResourceResolver resolver;

    public Locker(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isLocked(String lockName) {
        String name = normalizeName(lockName);
        Resource lock = getLock(name);
        return lock != null && lock.isResourceType(RESOURCE_TYPE);
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
                dirResource = ResourceUtils.makeFolders(resolver, ROOT + "/" + dirPath);
            } else {
                dirResource = ResourceUtils.makeFolders(resolver, ROOT);
                nodeName = name;
            }
            resolver.create(
                    dirResource, nodeName, Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, RESOURCE_TYPE));
            resolver.commit();
            LOG.debug("Created lock '{}'", name);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create lock '%s'!", name), e);
        }
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
}
