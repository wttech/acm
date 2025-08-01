package dev.vml.es.acm.core.script;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ResourceSpliterator;
import dev.vml.es.acm.core.util.ResourceUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptRepository {

    public static final String ROOT = AcmConstants.SETTINGS_ROOT + "/script";

    private static final Logger LOG = LoggerFactory.getLogger(ScriptRepository.class);

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
        return ResourceUtils.makeFolders(resourceResolver, type.root());
    }

    public void enable(String path) throws AcmException {
        Script script = read(path).orElse(null);
        if (script == null) {
            throw new AcmException(String.format("Script at path '%s' does not exist!", path));
        }
        if (script.getType() != ScriptType.DISABLED) {
            throw new AcmException(String.format("Script at path '%s' is not disabled!", path));
        }
        try {
            String sourcePath = script.getPath();
            String targetPath = ScriptType.ENABLED.enforcePath(path);
            ensureParentFolder(targetPath);
            ResourceUtils.move(resourceResolver, sourcePath, targetPath);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot enable script at path '%s'!", path), e);
        }
    }

    public void disable(String path) throws AcmException {
        Script script = read(path).orElse(null);
        if (script == null) {
            throw new AcmException(String.format("Script at path '%s' does not exist!", path));
        }
        if (script.getType() != ScriptType.ENABLED) {
            throw new AcmException(String.format("Script at path '%s' is not enabled!", path));
        }
        try {
            String sourcePath = script.getPath();
            String targetPath = ScriptType.DISABLED.enforcePath(path);
            ensureParentFolder(targetPath);
            ResourceUtils.move(resourceResolver, sourcePath, targetPath);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot disable script at path '%s'!", path), e);
        }
    }

    private void ensureParentFolder(String path) {
        try {
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            ResourceUtil.getOrCreateResource(
                    resourceResolver,
                    parentPath,
                    JcrResourceConstants.NT_SLING_ORDERED_FOLDER,
                    JcrResourceConstants.NT_SLING_ORDERED_FOLDER,
                    true);
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot create parent folder for path '%s'!", path), e);
        }
    }

    public void clean() {
        List<String> disabledPaths =
                findAll(ScriptType.DISABLED).map(Script::getPath).collect(Collectors.toList());
        List<Script> duplicatedScripts = findAll(ScriptType.ENABLED)
                .filter(s -> disabledPaths.contains(ScriptType.DISABLED.enforcePath(s.getPath())))
                .collect(Collectors.toList());

        if (!duplicatedScripts.isEmpty()) {
            String duplicatedPaths =
                    duplicatedScripts.stream().map(Script::getPath).collect(Collectors.joining(","));
            LOG.warn("Detected script duplicates ({}): {}", duplicatedScripts.size(), duplicatedPaths);
            LOG.warn(
                    "Scripts cannot be enabled & disabled at the same time so check Vault filters of package with scripts deployed to instance.");
            duplicatedScripts.forEach(s -> delete(s.getPath()));
            LOG.info("Removed script duplicates ({}): {}", duplicatedScripts.size(), duplicatedPaths);
        }
    }

    private void delete(String path) {
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            throw new AcmException(
                    String.format("Cannot delete script at path '%s' as it does not exist already!", path));
        }
        try {
            resourceResolver.delete(resource);
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new AcmException(String.format("Cannot delete script at path '%s'!", path), e);
        }
    }
}
