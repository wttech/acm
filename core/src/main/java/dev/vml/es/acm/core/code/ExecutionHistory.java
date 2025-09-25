package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.output.HistoryOutput;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.repo.RepoResource;
import dev.vml.es.acm.core.repo.RepoUtils;
import dev.vml.es.acm.core.util.StreamUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import javax.jcr.query.Query;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public class ExecutionHistory {

    public static final String ROOT = AcmConstants.VAR_ROOT + "/execution/history";

    public static final String OUTPUT_FILES_CONTAINER_RN = "outputFiles";

    public static final String OUTPUT_FILE_RN = "file";

    private final ResourceResolver resourceResolver;

    public ExecutionHistory(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Stream<Execution> read() {
        return Stream.empty();
    }

    public void save(ExecutionContext context, ImmediateExecution execution) throws AcmException {
        Resource root = getOrCreateRoot();
        Resource entry = saveEntry(context, execution, root);
        saveOutputs(context, execution, entry);
    }

    private Resource saveEntry(ExecutionContext context, ImmediateExecution execution, Resource root) {
        Map<String, Object> props = HistoricalExecution.toMap(context, execution);

        try {
            String dirPath = root.getPath() + "/" + StringUtils.substringBeforeLast(execution.getId(), "/");
            Resource dir = RepoUtils.ensure(resourceResolver, dirPath, JcrResourceConstants.NT_SLING_FOLDER, true);
            String entryName = StringUtils.substringAfterLast(execution.getId(), "/");
            Resource resource = resourceResolver.create(dir, entryName, props);
            resourceResolver.commit();
            return resource;
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Failed to save execution '%s'", execution.getId()), e);
        } finally {
            props.values().forEach(value -> {
                if (value instanceof Closeable) {
                    try {
                        ((Closeable) value).close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
        }
    }

    private void saveOutputs(ExecutionContext context, ImmediateExecution execution, Resource entry) {
        for (Output outputDefinition : context.getOutputs().getDefinitions().values()) {
            if (outputDefinition instanceof HistoryOutput) {
                HistoryOutput historyDefinition = (HistoryOutput) outputDefinition;
                RepoResource container = Repo.quiet(entry.getResourceResolver())
                        .get(entry.getPath())
                        .child(String.format("%s/%s", OUTPUT_FILES_CONTAINER_RN, historyDefinition.getName()))
                        .ensure(JcrConstants.NT_UNSTRUCTURED); // TODO fix ClassCastException
                RepoResource file = container.child(OUTPUT_FILE_RN);
                file.saveFile(outputDefinition.getMimeType(), outputDefinition.getInputStream());
            }
        }
    }

    public InputStream readOutputByName(Execution execution, String name) {
        return Repo.quiet(resourceResolver)
                .get(ROOT)
                .child(execution.getId())
                .child(String.format("%s/%s", OUTPUT_FILES_CONTAINER_RN, name))
                .child(OUTPUT_FILE_RN)
                .readFileAsStream();
    }

    public Optional<Execution> read(String id) {
        return readResource(id).map(HistoricalExecution::new);
    }

    public Optional<ExecutionSummary> readSummary(String id) {
        return readResource(id).map(HistoricalExecutionSummary::new);
    }

    private Optional<Resource> readResource(String id) {
        return Optional.of(getOrCreateRoot()).map(r -> r.getChild(id));
    }

    private Resource getOrCreateRoot() throws AcmException {
        return RepoUtils.ensure(resourceResolver, ROOT, JcrResourceConstants.NT_SLING_FOLDER, true);
    }

    public boolean contains(String id) {
        return getOrCreateRoot().getChild(id) != null;
    }

    public Stream<Execution> readAll(Collection<String> ids) {
        return Optional.ofNullable(ids).orElse(Collections.emptyList()).stream()
                .filter(StringUtils::isNotBlank)
                .map(this::read)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }


    public Optional<Execution> findById(String id) {
        ExecutionQuery query = new ExecutionQuery();
        query.setId(id);
        return findAll(query).findFirst();
    }

    public Stream<Execution> findAll() {
        return findAll(new ExecutionQuery());
    }

    public Stream<Execution> findAll(ExecutionQuery query) {
        return toExecutions(executeSql(query.toSql()));
    }

    private Stream<Execution> toExecutions(Stream<Resource> entries) {
        return entries.map(HistoricalExecution::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<ExecutionSummary> findAllSummaries() {
        return findAllSummaries(new ExecutionQuery());
    }

    public Stream<ExecutionSummary> findAllSummaries(ExecutionQuery query) {
        return toExecutionSummaries(executeSql(query.toSql()));
    }

    private Stream<ExecutionSummary> toExecutionSummaries(Stream<Resource> entries) {
        return entries.map(HistoricalExecutionSummary::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Stream<Resource> executeSql(String sql) {
        return StreamUtils.asStream(resourceResolver.findResources(sql, Query.JCR_SQL2));
    }

    public boolean clear() {
        try {
            Resource root = resourceResolver.getResource(ROOT);
            if (root != null) {
                resourceResolver.delete(root);
                resourceResolver.commit();
                return true;
            }
            return false;
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot clear execution history at root '%s'!", ROOT), e);
        }
    }
}
