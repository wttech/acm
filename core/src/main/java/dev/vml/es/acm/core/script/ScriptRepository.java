package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.Code;
import dev.vml.es.acm.core.code.script.ScriptUtils;
import dev.vml.es.acm.core.repo.RepoException;
import dev.vml.es.acm.core.repo.RepoResource;
import dev.vml.es.acm.core.repo.RepoUtils;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
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

    private Resource getOrCreateRoot(ScriptType type) throws RepoException {
        return RepoUtils.ensure(resourceResolver, type.root(), JcrResourceConstants.NT_SLING_FOLDER, true);
    }

    public Script save(Code code) {
        return save(code.getId(), code.getContent());
    }

    public Script save(String id, Object data) throws AcmException {
        if (!ScriptType.byPath(id).isPresent()) {
            throw new AcmException(String.format("Cannot save script '%s' at unsupported path!", id));
        }

        if (read(id).isPresent()) {
            throw new AcmException(String.format("Cannot save script '%s' as it already exists!", id));
        }
        RepoResource resource = RepoResource.of(resourceResolver, id);
        resource.parent().ensureRegularFolder();
        resource.saveFile(ScriptUtils.MIME_TYPE, data);
        return read(id).orElseThrow(() -> new AcmException(String.format("Cannot read script '%s' after saving!", id)));
    }

    public void deleteAll(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        ids.forEach(this::delete);
    }

    public void delete(String id) {
        if (!ScriptType.byPath(id).isPresent()) {
            throw new AcmException(String.format("Cannot delete script '%s' at unsupported path!", id));
        }
        RepoResource resource = RepoResource.of(resourceResolver, id);
        if (!resource.exists()) {
            throw new AcmException(String.format("Cannot delete script '%s' as it does not exist!", id));
        }
        resource.delete();
    }
}
