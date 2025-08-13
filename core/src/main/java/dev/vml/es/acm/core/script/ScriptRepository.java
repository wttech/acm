package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public class ScriptRepository {

    public static final String ROOT = AcmConstants.SETTINGS_ROOT + "/script";

    private final ResourceResolver resourceResolver;

    public ScriptRepository(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public Optional<Script> read(String path) {
        return Optional.ofNullable(path)
                .filter(p -> ScriptType.byPath(p).isPresent())
                .map(resourceResolver::getResource)
                .flatMap(Script::from);
    }

    public Stream<Script> findAll(ScriptType type) throws AcmException {
        return ResourceSpliterator.stream(getOrCreateRoot(type))
                .map(Script::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<Script> readAll(List<String> ids) {
        return ids.stream()
                .filter(p -> ScriptType.byPath(p).isPresent())
                .map(resourceResolver::getResource) // ID is a path
                .map(Script::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Resource getOrCreateRoot(ScriptType type) throws AcmException {
        try {
            Resource root = ResourceUtils.ensure(resourceResolver, type.root(), JcrResourceConstants.NT_SLING_FOLDER);
            resourceResolver.commit();
            return root;
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create script root '%s'!", type.root()), e);
        }

    }
}
