package com.wttech.aem.contentor.core.script;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ResourceSpliterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
                .map(r -> Script.from(r).orElse(null))
                .filter(Objects::nonNull);
    }

    public Stream<Script> readAll(List<String> paths) {
        return paths.stream()
                .filter(p -> ScriptType.byPath(p).isPresent())
                .map(resourceResolver::getResource)
                .map(Script::from)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Resource readRoot(ScriptType type) throws ContentorException {
        Resource root = resourceResolver.getResource(type.root());
        if (root == null) {
            throw new ContentorException(String.format("Script root path '%s' does not exist!", ROOT));
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
            String targetPath = ScriptType.ENABLED.root() + "/" + StringUtils.removeStart(path, ScriptType.DISABLED.root() + "/");
            resourceResolver.move(sourcePath, StringUtils.substringBeforeLast(targetPath, "/"));
            resourceResolver.commit();
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
            String targetPath = ScriptType.DISABLED.root() + "/" + StringUtils.removeStart(path, ScriptType.ENABLED.root() + "/");
            resourceResolver.move(sourcePath, StringUtils.substringBeforeLast(targetPath, "/"));
            resourceResolver.commit();
        } catch (PersistenceException e) {
            throw new ContentorException(String.format("Cannot disable script at path '%s'!", path), e);
        }
    }
}
