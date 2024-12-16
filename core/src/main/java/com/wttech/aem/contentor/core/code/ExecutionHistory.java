package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceSpliterator;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ExecutionHistory {

    public static final String ROOT = "/var/contentor/execution/history";

    private final ResourceResolver resourceResolver;

    public ExecutionHistory(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Stream<Execution> read() {
        return Stream.empty();
    }

    public void save(Execution execution) throws ContentorException {
        Resource parent = getOrCreateRoot();
        Map<String, Object> props = HistoricalExecution.toMap(execution);

        try {
            resourceResolver.create(parent, execution.getId(), props);
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new ContentorException(String.format("Failed to save execution '%s'", execution.getId()), e);
        }
    }

    public Execution read(String id) {
        return Optional.of(getOrCreateRoot())
                .map(r -> r.getChild(id))
                .map(HistoricalExecution::new)
                .orElse(null);
    }

    private Resource getOrCreateRoot() throws ContentorException {
        try {
            return ResourceUtil.getOrCreateResource(resourceResolver, ROOT, JcrResourceConstants.NT_SLING_FOLDER, JcrResourceConstants.NT_SLING_FOLDER, true);
        } catch (Exception e) {
            throw new ContentorException(String.format("Failed to get or create execution history root '%s'", ROOT), e);
        }
    }

    public boolean contains(String id) {
        return getOrCreateRoot().getChild(id) != null;
    }

    public Stream<Execution> readAll(Collection<String> ids) {
        return findAll().filter(e -> ids.contains(e.getId()));
    }

    public Stream<Execution> findAll() {
        return ResourceSpliterator.stream(getOrCreateRoot())
                .map(r -> HistoricalExecution.from(r).orElse(null))
                .filter(Objects::nonNull)
                .map(Execution.class::cast);
    }
}
