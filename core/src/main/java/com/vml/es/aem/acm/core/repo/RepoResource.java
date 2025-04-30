package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.util.ResourceSpliterator;
import com.vml.es.aem.acm.core.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoResource {

    private static final Logger LOG = LoggerFactory.getLogger(RepoResource.class);

    private final Repo repo;

    private final String path;

    public RepoResource(Repo repo, String path) {
        this.repo = repo;
        this.path = path;
    }

    public static RepoResource of(Resource resource) {
        return new Repo(resource.getResourceResolver()).get(resource.getPath());
    }

    public String getPath() {
        return path;
    }

    public boolean exists() {
        return repo.getResourceResolver().getResource(path) != null;
    }

    public boolean existsStrict(String path) {
        try {
            return repo.getSession().nodeExists(path);
        } catch (Exception e) {
            throw new RepoException(String.format("Resource at path '%s' cannot be checked for existence!", path), e);
        }
    }

    public RepoResource ensureFolder() {
        return ensure(JcrResourceConstants.NT_SLING_FOLDER);
    }

    public RepoResource ensureOrderedFolder() {
        return ensure(JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
    }

    public RepoResource ensure(String resourceType) {
        Resource resource = repo.getResourceResolver().getResource(path);
        if (resource == null) {
            try {
                ResourceUtil.getOrCreateResource(repo.getResourceResolver(), path, resourceType, resourceType, false);
            } catch (PersistenceException e) {
                throw new RepoException(
                        String.format("Cannot ensure resource '%s' at path '%s'!", resourceType, path), e);
            }
            repo.commit(String.format("ensuring resource '%s' at path %s", resourceType, path));
            LOG.info("Ensured resource '{}' at path '{}'", resourceType, path);
        } else {
            LOG.info("Skipped ensuring resource '{}' at path '{}'", resourceType, path);
        }
        return this;
    }

    public Resource save(Map<String, Object> values) {
        Resource result = repo.getResourceResolver().getResource(path);
        if (result == null) {
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            Resource parent = repo.getResourceResolver().getResource(parentPath);
            if (parent == null) {
                throw new RepoException(
                        String.format("Cannot save resource as parent path '%s' does not exist!", parentPath));
            }
            try {
                String name = StringUtils.substringAfterLast(path, "/");
                result = repo.getResourceResolver().create(parent, name, values);
                repo.commit(String.format("creating resource at path '%s'", path));
                LOG.info("Created resource at path '{}'", path);
                return result;
            } catch (PersistenceException e) {
                throw new RepoException(String.format("Cannot save resource at path '%s'!", path), e);
            }
        } else {
            ModifiableValueMap valueMap = Objects.requireNonNull(result.adaptTo(ModifiableValueMap.class));
            valueMap.putAll(values);
            repo.commit(String.format("updating resource at path '%s'", path));
            LOG.info("Updated resource at path '{}'", path);
        }
        return result;
    }

    public ValueMap properties() {
        return repo.requireResource(path).getValueMap();
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

    public void saveProperty(String key, Object value) {
        Resource resource = repo.getResourceResolver().getResource(path);
        if (resource == null) {
            throw new RepoException(
                    String.format("Cannot save property '%s' as resource at path '%s' does not exist!", key, path));
        }
        ModifiableValueMap props = Objects.requireNonNull(resource.adaptTo(ModifiableValueMap.class));
        Object valueExisting = props.get(key);
        if (Objects.equals(value, valueExisting)) {
            LOG.info(
                    "Skipped saving property '{}' for resource at path '{}' as it already exists with the same value '{}'!",
                    key,
                    path,
                    value);
            return;
        }
        props.put(key, value);

        if (valueExisting == null) {
            LOG.info("Created property '{}' with value '{}' for resource at path '{}'", key, value, path);
            repo.commit(String.format("creating property '%s' at path '%s'", key, path));
        } else {
            LOG.info(
                    "Updated property '{}' from value '{}' to '{}' for resource at path '{}'",
                    key,
                    valueExisting,
                    value,
                    path);
            repo.commit(String.format("updating property '%s' at path '%s'", key, path));
        }
    }

    public boolean delete() {
        Resource resource = repo.getResourceResolver().getResource(path);
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
        return true;
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
            throw new RepoException("Repo child resource name cannot be blank!");
        }
        String childPath = String.format("%s/%s", path, name);
        return new RepoResource(repo, childPath);
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
        return StreamUtils.asStream(repo.requireResource(path).listChildren())
                .map(r -> new RepoResource(repo, r.getPath()));
    }

    public Stream<RepoResource> traverse(boolean includeSelf) {
        Stream<RepoResource> result =
                ResourceSpliterator.stream(repo.requireResource(path)).map(r -> new RepoResource(repo, r.getPath()));
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

    public Stream<RepoResource> query(String nodeType, String whereSpec) {
        return repo.query(path, nodeType, whereSpec);
    }

    public Stream<RepoResource> query(String nodeType, String whereSpec, String orderBySpec) {
        return repo.query(path, nodeType, whereSpec, orderBySpec);
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
        return new RepoResourceState(this);
    }

    public Resource saveFile(Object data, String mimeType) {
        Resource mainResource = repo.getResourceResolver().getResource(path);
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
                setFileContent(contentValues, data, mimeType);
                repo.getResourceResolver().create(mainResource, JcrConstants.JCR_CONTENT, contentValues);

                repo.commit(String.format("creating file at path '%s'", path));
                LOG.info("Created file at path '{}'", path);
            } else {
                Resource contentResource = mainResource.getChild(JcrConstants.JCR_CONTENT);
                if (contentResource == null) {
                    Map<String, Object> contentValues = new HashMap<>();
                    setFileContent(contentValues, data, mimeType);
                    repo.getResourceResolver().create(mainResource, JcrConstants.JCR_CONTENT, contentValues);
                } else {
                    ModifiableValueMap contentValues =
                            Objects.requireNonNull(contentResource.adaptTo(ModifiableValueMap.class));
                    setFileContent(contentValues, data, mimeType);
                }

                repo.commit(String.format("updating file at path '%s'", path));
                LOG.info("Updated file at path '{}'", path);
            }
        } catch (PersistenceException e) {
            throw new RepoException(String.format("Cannot save file at path '%s'!", path), e);
        }
        return mainResource;
    }

    private void setFileContent(Map<String, Object> contentValues, Object data, String mimeType) {
        contentValues.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        contentValues.put(JcrConstants.JCR_ENCODING, "utf-8");
        contentValues.put(JcrConstants.JCR_DATA, data);
        contentValues.put(JcrConstants.JCR_MIMETYPE, mimeType);
    }

    public InputStream readFileAsStream() {
        Resource resource = repo.requireResource(path);
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
        return repo.requireResource(path).isResourceType(resourceType);
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
        return state().toString();
    }
}
