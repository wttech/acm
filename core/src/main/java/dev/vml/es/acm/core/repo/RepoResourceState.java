package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.util.StringUtil;
import dev.vml.es.acm.core.util.TypeValueMap;
import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.ValueMap;

/**
 * Immutable snapshot of a repository resource state (even not existing).
 * May be used to compare with another resource state (before/after some operation).
 */
public class RepoResourceState implements Serializable {

    private final String path;

    private final boolean exists;

    private final ValueMap properties;

    public RepoResourceState(String path, boolean exists, Map<String, Object> properties) {
        this.path = path;
        this.exists = exists;
        this.properties = new TypeValueMap(properties);
    }

    public String getPath() {
        return path;
    }

    public boolean isExists() {
        return exists;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepoResourceState that = (RepoResourceState) o;
        return new EqualsBuilder()
                .append(path, that.path)
                .append(exists, that.exists)
                .append(properties, that.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(path)
                .append(exists)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("path", path)
                .append("exists", exists)
                .append("properties", StringUtil.toString(properties))
                .toString();
    }
}
