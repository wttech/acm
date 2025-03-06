package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceSpliterator;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public class ScriptRepository {

    public static final String ROOT = "/conf/contentor/settings/script";

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

    public Stream<Script> findAll(ScriptType type) throws ContentorException {
        return ResourceSpliterator.stream(readRoot(type))
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

    private Resource readRoot(ScriptType type) throws ContentorException {
        Resource root = resourceResolver.getResource(type.root());
        if (root == null) {
            throw new ContentorException(String.format("Script root path '%s' does not exist!", type.root()));
        }
        return root;
    }

    public void enable(String path) throws ContentorException {
        Script script = read(path).orElse(null);
        if (script == null) {
            throw new ContentorException(String.format("Script at path '%s' does not exist!", path));
        }
        if (script.getType() == ScriptType.ENABLED) {
            throw new ContentorException(String.format("Script at path '%s' is already enabled!", path));
        }
        try {
            String sourcePath = script.getPath();
            String targetPath = ScriptType.ENABLED.enforcePath(path);
            ensureParentFolder(targetPath);
            ResourceUtils.move(resourceResolver, sourcePath, targetPath);
        } catch (PersistenceException e) {
            throw new ContentorException(String.format("Cannot enable script at path '%s'!", path), e);
        }
    }

    public void disable(String path) throws ContentorException {
        Script script = read(path).orElse(null);
        if (script == null) {
            throw new ContentorException(String.format("Script at path '%s' does not exist!", path));
        }
        if (script.getType() == ScriptType.DISABLED) {
            throw new ContentorException(String.format("Script at path '%s' is already disabled!", path));
        }
        try {
            String sourcePath = script.getPath();
            String targetPath = ScriptType.DISABLED.enforcePath(path);
            ensureParentFolder(targetPath);
            ResourceUtils.move(resourceResolver, sourcePath, targetPath);
        } catch (PersistenceException e) {
            throw new ContentorException(String.format("Cannot disable script at path '%s'!", path), e);
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
            throw new ContentorException(String.format("Cannot create parent folder for path '%s'!", path), e);
        }
    }
}
