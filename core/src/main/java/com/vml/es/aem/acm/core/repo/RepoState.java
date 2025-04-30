package com.vml.es.aem.acm.core.repo;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.ValueMap;

public class RepoState implements Serializable {

    private final String path;

    private final boolean exists;

    private final ValueMap properties;

    public RepoState(String path, boolean exists, ValueMap properties) {
        this.path = path;
        this.exists = exists;
        this.properties = properties;
    }

    public String getPath() {
        return path;
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
        RepoState that = (RepoState) o;
        return new EqualsBuilder()
                .append(exists, that.exists)
                .append(path, that.path)
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
                .append("properties", properties)
                .toString();
    }
}
