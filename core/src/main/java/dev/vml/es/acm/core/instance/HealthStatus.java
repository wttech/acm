package dev.vml.es.acm.core.instance;

import dev.vml.es.acm.core.util.ExceptionUtils;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class HealthStatus implements Serializable {

    private boolean healthy;

    private List<HealthIssue> issues = new LinkedList<>();

    public static HealthStatus exception(Exception e) {
        HealthStatus result = new HealthStatus();
        result.healthy = false;
        result.issues.add(new HealthIssue(
                HealthIssueSeverity.CRITICAL, HealthIssueCategory.OTHER, "Internal error", ExceptionUtils.toString(e)));
        return result;
    }

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
