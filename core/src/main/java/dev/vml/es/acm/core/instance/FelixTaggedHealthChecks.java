package dev.vml.es.acm.core.instance;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** Stays unsatisfied, and its class unloaded, where the Felix Health Check API is missing (classic AEM 6.5). */
@Component(service = TaggedHealthChecks.class)
public class FelixTaggedHealthChecks implements TaggedHealthChecks {

    @Reference
    private HealthCheckExecutor executor;

    @Override
    public List<HealthIssue> findCriticalIssues(String[] tags) {
        HealthCheckExecutionOptions anyOfTags = new HealthCheckExecutionOptions().setCombineTagsWithOr(true);
        return executor.execute(HealthCheckSelector.tags(tags), anyOfTags).stream()
                .filter(result -> result.getHealthCheckResult().getStatus() == Result.Status.CRITICAL)
                .map(this::toIssue)
                .collect(Collectors.toList());
    }

    private HealthIssue toIssue(HealthCheckExecutionResult result) {
        return new HealthIssue(
                HealthIssueSeverity.CRITICAL,
                HealthIssueCategory.INSTANCE,
                String.format(
                        "Health check critical: %s",
                        result.getHealthCheckMetadata().getName()),
                describe(result.getHealthCheckResult()));
    }

    /** Debug entries come first and would hide the reason. */
    private String describe(Result result) {
        return StreamSupport.stream(result.spliterator(), false)
                .filter(entry -> !entry.isDebug())
                .map(ResultLog.Entry::getMessage)
                .collect(Collectors.joining("; "));
    }
}
