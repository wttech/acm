package dev.vml.es.acm.core.instance;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class HealthIssue implements Serializable {

    private final HealthIssueSeverity severity;

    private final HealthIssueCategory category;

    private final String issue;

    private final String details;

    public HealthIssue(HealthIssueSeverity severity, HealthIssueCategory category, String issue, String details) {
        this.severity = severity;
        this.issue = issue;
        this.category = category;
        this.details = details;
    }

    public HealthIssueSeverity getSeverity() {
        return severity;
    }

    public HealthIssueCategory getCategory() {
        return category;
    }

    public String getIssue() {
        return issue;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("severity", severity)
                .append("issue", issue)
                .append("category", category)
                .append("details", StringUtils.abbreviate(details, 200))
                .toString();
    }
}
