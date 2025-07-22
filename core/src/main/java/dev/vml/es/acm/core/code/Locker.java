package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;

public class Locker {

    public static final String ROOT = AcmConstants.VAR_ROOT + "/lock";

    private static final Logger LOG = LoggerFactory.getLogger(Locker.class);

    private static final String CREATED_PROP = "created";

    private final ResourceResolver resolver;

    public Locker(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isLocked(String name) {
        return getLock(name) != null;
    }

    public void lock(String name) {
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
            resolver.create(dirResource, nodeName, Collections.singletonMap(CREATED_PROP, new Date()));
            LOG.debug("Created lock '{}'", name);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create lock '%s'!", name), e);
        }
    }

    public void unlock(String name) {
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

    private Resource getLock(String name) {
        if (StringUtils.isBlank(name)) {
            throw new AcmException("Lock name cannot be blank!");
        }
        return resolver.getResource(ROOT + "/" + name);
    }
}
