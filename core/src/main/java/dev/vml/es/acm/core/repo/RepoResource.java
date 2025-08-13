package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fluent wrapper for Sling/JCR resources, simplifying common repository operations.
 * <p>
 * This class abstracts and streamlines the standard AEM and Sling APIs, which are often verbose and complex,
 * by providing a concise and developer-friendly interface for resource management tasks such as creation,
 * retrieval, update, deletion, traversal, and property manipulation.
 * <p>
 * All operations are performed using the underlying {@link ResourceResolver} and JCR session, supporting both
 * transient and committed changes. This enables efficient scripting, automation, and integration scenarios
 * within AEM and Sling-based applications.
 */
public class RepoResource {

    private static final Logger LOG = LoggerFactory.getLogger(RepoResource.class);

    private final Repo repo;

    private final String path;

    public RepoResource(Repo repo, String path) {
        this.repo = repo;
        this.path = path;
    }

    public static RepoResource of(ResourceResolver resourceResolver, String path) {
        return new RepoResource(new Repo(resourceResolver), path);
    }

    public static RepoResource of(Resource resource) {
        return new Repo(resource.getResourceResolver()).get(resource.getPath());
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return StringUtils.substringAfterLast(path, "/");
    }

    public int getDepth() {
        return StringUtils.countMatches(path, "/");
    }

    public Optional<Resource> get() {
        return Optional.ofNullable(repo.getResourceResolver().getResource(path));
    }

    public Resource resolve() {
        return get().orElse(null);
    }

    public Resource require() {
        return get().orElseThrow(() -> new RepoException(String.format("Resource at path '%s' does not exist!", path)));
    }

    public boolean exists() {
        return get().isPresent();
    }

    public RepoResource ensureFolder() {
        return ensure(JcrConstants.NT_FOLDER);
    }

    public RepoResource ensureRegularFolder() {
        return ensure(JcrResourceConstants.NT_SLING_FOLDER);
    }

    public RepoResource ensureOrderedFolder() {
        return ensure(JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
    }

    public RepoResource ensure(String primaryType) {
        Resource resource = resolve();
        if (resource == null) {
            try {
                ResourceUtils.ensure(repo.getResourceResolver(), path, primaryType);
            } catch (Exception e) {
                throw new RepoException(
                        String.format("Cannot ensure resource '%s' at path '%s'!", primaryType, path), e);
            }
            repo.commit(String.format("ensuring resource '%s' at path %s", primaryType, path));
            LOG.info("Ensured resource '{}' at path '{}'", primaryType, path);
        } else {
            LOG.info("Skipped ensuring resource '{}' at path '{}'", primaryType, path);
        }
        return this;
    }

    public RepoResource save(String key, Object value) {
        save(Collections.singletonMap(key, value));
        return this;
    }

    public RepoResource save(Map<String, Object> values) {
        Resource resource = resolve();
        if (resource == null) {
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            Resource parent = repo.getResourceResolver().getResource(parentPath);
            if (parent == null) {
                throw new RepoException(
                        String.format("Cannot save resource as parent path '%s' does not exist!", parentPath));
            }
            try {
                String name = StringUtils.substringAfterLast(path, "/");
                repo.getResourceResolver().create(parent, name, values);
                repo.commit(String.format("creating resource at path '%s'", path));
                LOG.info("Created resource at path '{}'", path);
                return this;
            } catch (PersistenceException e) {
                throw new RepoException(String.format("Cannot save resource at path '%s'!", path), e);
            }
        } else {
            ModifiableValueMap valueMap = Objects.requireNonNull(resource.adaptTo(ModifiableValueMap.class));
            valueMap.putAll(values);
            repo.commit(String.format("updating resource at path '%s'", path));
            LOG.info("Updated resource at path '{}'", path);
        }
        return this;
    }

    public ValueMap properties() {
        return new TypeValueMap(require().getValueMap());
    }

    private ValueMap propertiesOrEmpty() {
        return new TypeValueMap(get().map(Resource::getValueMap).orElse(ValueMap.EMPTY));
    }

    public <V> V property(String key, Class<V> clazz) {
        return properties().get(key, clazz);
    }

    public <V> V property(String key, V defaultValue) {
        return properties().get(key, defaultValue);
    }

    public Object property(String key) {
        return properties().get(key);
    }

    public boolean hasProperty(String key) {
        return properties().containsKey(key);
    }

    public RepoResource updateProperty(String key, Function<Object, Object> valueUpdater) {
        Resource resource = resolve();
        if (resource == null) {
            throw new RepoException(
                    String.format("Cannot save property '%s' as resource at path '%s' does not exist!", key, path));
        }
        ModifiableValueMap props = Objects.requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        Object valueExisting = props.get(key);
        Object valueUpdated = valueUpdater.apply(valueExisting);
        if (Objects.equals(valueUpdated, valueExisting)) {
            LOG.info(
                    "Skipped saving property '{}' for resource at path '{}' as it already exists with the same value '{}'!",
                    key,
                    path,
                    valueUpdated);
            return this;
        }

        if (valueUpdated == null) {
            props.remove(key);
            LOG.info("Deleted property '{}' with value '{}' for resource at path '{}'", key, valueExisting, path);
            repo.commit(String.format("deleting property '%s' at path '%s'", key, path));
        } else if (valueExisting == null) {
            props.put(key, valueUpdated);
            LOG.info("Created property '{}' with value '{}' for resource at path '{}'", key, valueUpdated, path);
            repo.commit(String.format("creating property '%s' at path '%s'", key, path));
        } else {
            props.put(key, valueUpdated);
            LOG.info(
                    "Updated property '{}' from value '{}' to '{}' for resource at path '{}'",
                    key,
                    valueExisting,
                    valueUpdated,
                    path);
            repo.commit(String.format("updating property '%s' at path '%s'", key, path));
        }
        return this;
    }

    public RepoResource saveProperty(String key, Object value) {
        return updateProperty(key, v -> value);
    }

    public RepoResource saveProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            LOG.info("Skipped saving properties for resource at path '{}' as no properties were provided!", path);
            return this;
        }
        properties.forEach(this::saveProperty);
        return this;
    }

    public RepoResource deleteProperty(String key) {
        return saveProperty(key, null);
    }

    public boolean delete() {
        Resource resource = resolve();
        if (resource == null) {
            LOG.info("Skipped deletion as resource does not exist at path '{}'", path);
            return false;
        }
        try {
            repo.getResourceResolver().delete(resource);
        } catch (PersistenceException e) {
            throw new RepoException(String.format("Cannot delete resource at path '%s'!", path), e);
        }
        LOG.info("Deleted resource at path '{}'", path);
        repo.commit(String.format("deleting resource at path '%s'", path));
        return true;
    }

    public RepoResource copy(String targetPath) {
        return copy(targetPath, false);
    }

    public RepoResource copy(String targetPath, boolean replace) {
        return copy(new RepoResource(repo, targetPath), replace);
    }

    public RepoResource copy(RepoResource target) {
        return copy(target, false);
    }

    public RepoResource copy(RepoResource target, boolean replace) {
        if (StringUtils.equals(path, target.path)) {
            throw new RepoException(String.format("Cannot copy resource to itself at path '%s'!", path));
        }

        Resource sourceResource = require();
        RepoResource targetParentResource = target.parent();
        if (!targetParentResource.exists()) {
            throw new RepoException(String.format(
                    "Cannot copy resource from '%s' to '%s' as target parent does not exist!", path, target.getPath()));
        }
        if (target.exists()) {
            if (replace) {
                target.delete();
            } else {
                LOG.info("Skipped copying resource from '{}' to '{}' as it already exists", path, target.getPath());
                return target;
            }
        }

        ResourceUtils.copy(repo.getResourceResolver(), sourceResource.getPath(), target.getPath());
        repo.commit(String.format("copying resource from '%s' to '%s'", path, target.getPath()));
        LOG.info("Copied resource from '{}' to '{}'", path, target.getPath());
        return target;
    }

    public RepoResource move(String targetPath) {
        return move(targetPath, false);
    }

    public RepoResource move(String targetPath, boolean replace) {
        return move(new RepoResource(repo, targetPath), replace);
    }

    public RepoResource move(RepoResource target) {
        return move(target, false);
    }

    public RepoResource move(RepoResource target, boolean replace) {
        if (StringUtils.equals(path, target.path)) {
            throw new RepoException(String.format("Cannot move resource to itself at path '%s'!", path));
        }

        Resource sourceResource = require();
        RepoResource targetParentResource = target.parent();
        if (!targetParentResource.exists()) {
            throw new RepoException(String.format(
                    "Cannot move resource '%s' to '%s' as target parent does not exist!", path, target.getPath()));
        }

        if (target.exists()) {
            if (replace) {
                target.delete();
            } else {
                LOG.info("Skipped moving resource from '{}' to '{}' as it already exists", path, target.getPath());
                return target;
            }
        }

        try {
            repo.getSession().move(sourceResource.getPath(), target.getPath());
            repo.commit(String.format("moving resource from '%s' to '%s'", path, target.getPath()));
            LOG.info("Moved resource from '{}' to '{}'", path, target.getPath());
        } catch (RepositoryException e) {
            throw new RepoException(
                    String.format("Cannot move resource from '%s' to '%s'!", path, target.getPath()), e);
        }
        return target;
    }

    public RepoResource rename(String newName) {
        return rename(newName, false);
    }

    public RepoResource rename(String newName, boolean replace) {
        if (StringUtils.isBlank(newName)) {
            throw new IllegalArgumentException(
                    String.format("Cannot rename resource '%s' as new name is blank!", path));
        }
        String parent = parentPath();
        if (parent == null) {
            throw new RepoException(String.format("Cannot rename root resource '%s'!", path));
        }
        String targetPath = parent.endsWith("/") ? parent + newName : parent + "/" + newName;
        return move(targetPath, replace);
    }

    public RepoResource moveInPlace(RepoResource target) {
        return moveInPlace(target, false);
    }

    public RepoResource moveInPlace(RepoResource target, boolean replace) {
        if (!repo.isAutoCommit()) {
            throw new RepoException(
                    "Cannot move resource in place as auto-commit is disabled! This operation always does an immediate commit.");
        }

        if (target.exists()) {
            if (replace) {
                target.delete();
            } else {
                throw new RepoException(String.format(
                        "Cannot move in place resource '%s' to '%s' as it already exists!", path, target.getPath()));
            }
        }

        try {
            repo.getSession().getWorkspace().move(path, target.getPath());
            LOG.info("Moved resource in place from '{}' to '{}'", path, target.getPath());
        } catch (RepositoryException e) {
            throw new RepoException(
                    String.format("Cannot move resource in place from '%s' to '%s'!", path, target.getPath()), e);
        }
        return target;
    }

    public RepoResource parent() {
        String parentPath = parentPath();
        if (parentPath == null) {
            throw new RepoException(String.format("Root resource '%s' does not have parent!", path));
        }
        return new RepoResource(repo, parentPath);
    }

    public String parentPath() {
        if ("/".equals(path)) {
            return null;
        }
        return StringUtils.defaultIfEmpty(StringUtils.substringBeforeLast(path, "/"), "/");
    }

    public boolean hasParent() {
        return parentPath() != null;
    }

    public RepoResource child(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Repo child resource name cannot be blank!");
        }
        String childPath = String.format("%s/%s", path, name);
        return new RepoResource(repo, childPath);
    }

    public boolean isChild(String otherPath) {
        return isChild(new RepoResource(repo, otherPath));
    }

    public boolean isChild(RepoResource other) {
        return StringUtils.equals(path, other.parentPath()) && !StringUtils.equals(getName(), other.getName());
    }

    public RepoResource sibling(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Repo sibling resource name cannot be blank!");
        }
        String siblingPath = String.format("%s/%s", parentPath(), name);
        return new RepoResource(repo, siblingPath);
    }

    public boolean isSibling(String otherPath) {
        return isSibling(new RepoResource(repo, otherPath));
    }

    public boolean isSibling(RepoResource other) {
        return StringUtils.equals(parentPath(), other.parentPath()) && !StringUtils.equals(getName(), other.getName());
    }

    public Stream<RepoResource> siblings() {
        return siblings(true);
    }

    public Stream<RepoResource> siblings(boolean includeSelf) {
        return parent().children().filter(s -> includeSelf || !StringUtils.equals(s.getPath(), path));
    }

    public Stream<RepoResource> traverse() {
        return traverse(true);
    }

    public Stream<RepoResource> children() {
        return StreamUtils.asStream(require().listChildren()).map(r -> new RepoResource(repo, r.getPath()));
    }

    public boolean hasChildren() {
        return require().hasChildren();
    }

    public Stream<RepoResource> traverse(boolean includeSelf) {
        Stream<RepoResource> result =
                ResourceSpliterator.stream(require()).map(r -> new RepoResource(repo, r.getPath()));
        if (!includeSelf) {
            result = result.skip(1);
        }
        return result;
    }

    public Stream<RepoResource> query() {
        return repo.query(path);
    }

    public Stream<RepoResource> query(String nodeType) {
        return repo.query(path, nodeType);
    }

    public Stream<RepoResource> query(String nodeType, String where) {
        return repo.query(path, nodeType, where);
    }

    public Stream<RepoResource> query(String nodeType, String where, String orderBy) {
        return repo.query(path, nodeType, where, orderBy);
    }

    public Stream<RepoResource> descendants() {
        return traverse(false);
    }

    public Stream<RepoResource> ancestors() {
        return parents();
    }

    public Stream<RepoResource> parents() {
        List<RepoResource> parentList = new LinkedList<>();
        RepoResource current = this;
        while (current.hasParent()) {
            current = current.parent();
            parentList.add(current);
        }
        return parentList.stream();
    }

    public Stream<RepoResource> breadcrumb() {
        List<RepoResource> breadcrumbs = new LinkedList<>();
        RepoResource current = this;
        while (current != null) {
            breadcrumbs.add(0, current);
            current = current.hasParent() ? current.parent() : null;
        }
        return breadcrumbs.stream();
    }

    public RepoResourceState state() {
        Resource resource = resolve();
        if (resource == null) {
            return new RepoResourceState(path, false, Collections.emptyMap());
        }
        return new RepoResourceState(path, true, resource.getValueMap());
    }

    public RepoResource saveFile(String mimeType, Consumer<OutputStream> dataWriter) {
        saveFileInternal(mimeType, null, dataWriter);
        return this;
    }

    public RepoResource saveFile(String mimeType, File file) {
        saveFile(mimeType, (OutputStream os) -> {
            try (InputStream is = Files.newInputStream(file.toPath())) {
                IOUtils.copy(is, os);
            } catch (IOException e) {
                throw new RepoException(String.format("Cannot write file '%s' to path '%s'!", file.getPath(), path), e);
            }
        });
        return this;
    }

    public RepoResource saveFile(String mimeType, Object data) {
        saveFileInternal(mimeType, data, null);
        return this;
    }

    private void saveFileInternal(String mimeType, Object data, Consumer<OutputStream> dataWriter) {
        Resource mainResource = resolve();
        try {
            if (mainResource == null) {
                String parentPath = StringUtils.substringBeforeLast(path, "/");
                Resource parent = repo.getResourceResolver().getResource(parentPath);
                if (parent == null) {
                    throw new RepoException(
                            String.format("Cannot save file as parent path '%s' does not exist!", parentPath));
                }
                String name = StringUtils.substringAfterLast(path, "/");
                Map<String, Object> mainValues = new HashMap<>();
                mainValues.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
                mainResource = repo.getResourceResolver().create(parent, name, mainValues);

                Map<String, Object> contentValues = new HashMap<>();
                setFileContent(contentValues, mimeType, data, dataWriter);
                repo.getResourceResolver().create(mainResource, JcrConstants.JCR_CONTENT, contentValues);

                repo.commit(String.format("creating file at path '%s'", path));
                LOG.info("Created file at path '{}'", path);
            } else {
                Resource contentResource = mainResource.getChild(JcrConstants.JCR_CONTENT);
                if (contentResource == null) {
                    Map<String, Object> contentValues = new HashMap<>();
                    setFileContent(contentValues, mimeType, data, dataWriter);
                    repo.getResourceResolver().create(mainResource, JcrConstants.JCR_CONTENT, contentValues);
                } else {
                    ModifiableValueMap contentValues =
                            Objects.requireNonNull(contentResource.adaptTo(ModifiableValueMap.class));
                    setFileContent(contentValues, mimeType, data, dataWriter);
                }

                repo.commit(String.format("updating file at path '%s'", path));
                LOG.info("Updated file at path '{}'", path);
            }
        } catch (PersistenceException e) {
            throw new RepoException(String.format("Cannot save file at path '%s'!", path), e);
        }
    }

    private void setFileContent(
            Map<String, Object> contentValues, String mimeType, Object data, Consumer<OutputStream> dataWriter) {
        if (dataWriter != null) {
            setFileContent(contentValues, mimeType, dataWriter);
        } else {
            setFileContent(contentValues, mimeType, data);
        }
    }

    private void setFileContent(Map<String, Object> contentValues, String mimeType, Object data) {
        contentValues.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        contentValues.put(JcrConstants.JCR_ENCODING, "utf-8");
        contentValues.put(JcrConstants.JCR_MIMETYPE, mimeType);
        contentValues.put(JcrConstants.JCR_DATA, data);
    }

    // https://stackoverflow.com/a/27172165
    private void setFileContent(Map<String, Object> contentValues, String mimeType, Consumer<OutputStream> dataWriter) {
        contentValues.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        contentValues.put(JcrConstants.JCR_ENCODING, "utf-8");
        contentValues.put(JcrConstants.JCR_MIMETYPE, mimeType);
        try {
            final PipedInputStream pis = new PipedInputStream();
            final PipedOutputStream pos = new PipedOutputStream(pis);
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    dataWriter.accept(pos);
                } catch (Exception e) {
                    throw new RepoException(String.format("Cannot write data to file at path '%s'!", path), e);
                } finally {
                    try {
                        pos.close();
                    } catch (IOException e) {
                        LOG.warn("Cannot close output stream for file at path '{}'", path, e);
                    }
                }
            });
            contentValues.put(JcrConstants.JCR_DATA, pis);
        } catch (IOException e) {
            throw new RepoException(String.format("Cannot save file at path '%s'!", path), e);
        }
    }

    public InputStream readFileAsStream() {
        Resource resource = require();
        Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
        if (contentResource == null) {
            throw new RepoException(String.format("Cannot read file at path '%s' as it does not have content!", path));
        }
        InputStream inputStream = contentResource.adaptTo(InputStream.class);
        if (inputStream == null) {
            throw new RepoException(
                    String.format("Cannot read file at path '%s' as it does not have content stream!", path));
        }
        return inputStream;
    }

    public String readFileAsString() {
        try {
            return IOUtils.toString(readFileAsStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepoException(String.format("Cannot read file as path '%s' as string!", path), e);
        }
    }

    public boolean isFile() {
        return isType(JcrConstants.NT_FILE);
    }

    public boolean isType(String resourceType) {
        return require().isResourceType(resourceType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepoResource that = (RepoResource) o;
        return new EqualsBuilder().append(path, that.path).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(path).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", path)
                .append("exists", exists())
                .append("properties", StringUtil.toString(propertiesOrEmpty()))
                .toString();
    }
}
