package com.vml.es.aem.acm.core.repo;

import com.vml.es.aem.acm.core.util.StringUtil;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.ValueMap;

/**
 * Immutable snapshot of a repository resource state (even not existing).
 */
public class RepoResourceState implements Serializable {

    private final RepoResource resource;

    private final boolean exists;

    private final ValueMap properties;

    public RepoResourceState(RepoResource resource) {
        this.resource = resource;
        this.exists = resource.exists();
        this.properties = resource.properties();
    }

    public String getPath() {
        return resource.getPath();
    }

    public boolean isExists() {
        return exists;
    }

    public ValueMap getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepoResourceState that = (RepoResourceState) o;
        return new EqualsBuilder()
                .append(resource.getPath(), that.getPath())
                .append(exists, that.exists)
                .append(properties, that.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(resource.getPath())
                .append(exists)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(resource, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", resource.getPath())
                .append("exists", exists)
                .append("properties", StringUtil.toString(properties))
                .toString();
    }
}
