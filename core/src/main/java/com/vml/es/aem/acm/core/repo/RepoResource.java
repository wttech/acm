package com.vml.es.aem.acm.core.repo;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.Resource;

public class RepoResource {

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

    public void save(Map<String, Object> props) {
        repo.save(path, props);
    }

    public void saveProp(String key, Object value) {
        repo.saveProp(path, key, value);
    }

    public void saveFile(Object data, String mimeType) {
        repo.saveFile(path, data, mimeType);
    }

    public boolean isFile() {
        return repo.isFile(path);
    }

    public boolean isType(String resourceType) {
        return repo.isType(path, resourceType);
    }

    public String readAsString() {
        return repo.readFileAsString(path);
    }

    public InputStream readAsStream() {
        return repo.readFileAsStream(path);
    }

    public void delete() {
        repo.delete(path);
    }

    public boolean exists() {
        return repo.exists(path);
    }

    public void makeFolders() {
        repo.makeFolders(path);
    }

    public RepoResource parent() {
        String parentPath = parentPath();
        if (parentPath == null) {
            throw new RepoException(String.format("Root resource '%s' does not have parent!", path));
        }
        return new RepoResource(repo, parentPath);
    }

    public String parentPath() {
        return StringUtils.trimToNull(StringUtils.substringBeforeLast(path, "/"));
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

    public Stream<RepoResource> children() {
        return repo.children(path);
    }

    public Stream<RepoResource> siblings() {
        return parent().children().filter(s -> !StringUtils.equals(s.getPath(), path));
    }

    public Stream<RepoResource> recurse() {
        return repo.recurse(path);
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
        return recurse();
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
                .toString();
    }
}
