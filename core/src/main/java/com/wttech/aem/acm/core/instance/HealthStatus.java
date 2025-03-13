package com.wttech.aem.acm.core.instance;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class HealthStatus implements Serializable {

    boolean healthy;

    List<HealthIssue> issues = new LinkedList<>();

    public List<HealthIssue> getIssues() {
        return issues;
    }

    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("healthy", healthy)
                .append("issues", issues)
                .toString();
    }
}
