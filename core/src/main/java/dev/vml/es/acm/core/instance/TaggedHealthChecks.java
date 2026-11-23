package dev.vml.es.acm.core.instance;

import java.util.List;

/** AEM health checks selected by tags. Implemented only where the Felix Health Check API is available. */
public interface TaggedHealthChecks {

    List<HealthIssue> findCriticalIssues(String[] tags);
}
