package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceSpliterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.*;
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

    public void save(ImmediateExecution execution) throws ContentorException {
        Resource root = getOrCreateRoot();
        Map<String, Object> props = HistoricalExecution.toMap(execution);

        try {
            String dirPath = root.getPath() + "/" + StringUtils.substringBeforeLast(execution.getId(), "/");
            Resource dir = ResourceUtil.getOrCreateResource(resourceResolver, dirPath, JcrResourceConstants.NT_SLING_FOLDER, JcrResourceConstants.NT_SLING_FOLDER, true);
            String entryName = StringUtils.substringAfterLast(execution.getId(), "/");
            resourceResolver.create(dir, entryName, props);
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new ContentorException(String.format("Failed to save execution '%s'", execution.getId()), e);
        }
    }

    public Optional<Execution> read(String id) {
        return Optional.of(getOrCreateRoot())
                .map(r -> r.getChild(id))
                .map(HistoricalExecution::new);
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
        return Optional.ofNullable(ids)
                .orElse(Collections.emptyList())
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(this::read)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<Execution> findAll() {
        return ResourceSpliterator.stream(getOrCreateRoot())
                .map(r -> HistoricalExecution.from(r).orElse(null))
                .filter(Objects::nonNull)
                .map(Execution.class::cast);
    }
}
