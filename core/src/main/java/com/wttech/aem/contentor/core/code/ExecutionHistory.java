package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ExecutionHistory {

    public static final String ROOT = "/var/contentor/execution/history";

    public static final String ENTRY_RT = "nt:unstructured";

    public static final String FOLDER_RT = "sling:Folder";

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
        props.put(JcrConstants.JCR_PRIMARYTYPE, ENTRY_RT);
        props.entrySet().removeIf(e -> e.getValue() == null);
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
            return ResourceUtil.getOrCreateResource(resourceResolver, ROOT, FOLDER_RT, FOLDER_RT, true);
        } catch (Exception e) {
            throw new ContentorException(String.format("Failed to get or create execution history root '%s'", ROOT), e);
        }
    }

    public boolean contains(String id) {
        return getOrCreateRoot().getChild(id) != null;
    }
}
