package com.wttech.aem.contentor.core.instance;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class HealthStatus {

    private boolean healthy;

    private List<HealthIssue> issues = new LinkedList<>();

    public static HealthStatus mock() {
        HealthStatus result = new HealthStatus();
        result.issues.add(new HealthIssue("OSGi bundle 'com.acme.aem.core' is not active!"));
        return result;
    }

    public List<HealthIssue> getIssues() {
        return issues;
    }

    public boolean isHealthy() {
        return CollectionUtils.isEmpty(issues);
    }
}
