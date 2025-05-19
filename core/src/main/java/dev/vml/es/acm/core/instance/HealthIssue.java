package dev.vml.es.acm.core.instance;

import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class HealthIssue implements Serializable {

    private final String message;

    private final HealthIssueSeverity severity;

    public HealthIssue(HealthIssueSeverity severity, String message) {
        this.severity = severity;
        this.message = message;
    }

    public HealthIssueSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("severity", severity)
                .append("message", message)
                .toString();
    }
}
