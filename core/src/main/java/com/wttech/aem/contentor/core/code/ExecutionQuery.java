package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.util.DateUtils;
import com.wttech.aem.contentor.core.util.ServletUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExecutionQuery {

    private String path = ExecutionHistory.ROOT;

    private Date startDate;

    private Date endDate;

    private ExecutionStatus status;

    public static ExecutionQuery from(SlingHttpServletRequest request) {
        ExecutionQuery result = new ExecutionQuery();
        result.setStartDate(DateUtils.fromString(ServletUtils.stringParam(request, "startDate")));
        result.setEndDate(DateUtils.fromString(ServletUtils.stringParam(request, "endDate")));
        result.setStatus(ExecutionStatus.of(ServletUtils.stringParam(request, "status")).orElse(null));
        return result;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (!StringUtils.startsWith(path, ExecutionHistory.ROOT)) {
            throw new IllegalArgumentException(String.format("Path must be a descendant of '%s'!", ExecutionHistory.ROOT));
        }
        this.path = path;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    protected String toSql() {
        List<String> filters = new ArrayList<>();
        filters.add(String.format("s.[%s] = '%s'", JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, HistoricalExecution.RESOURCE_TYPE));
        if (path != null) {
            filters.add(String.format("ISDESCENDANTNODE(s, '%s')", path));
        }
        if (status != null) {
            filters.add(String.format("s.[status] = '%s'", status));
        }
        if (startDate != null) {
            filters.add(String.format("s.[startDate] >= CAST('%s' AS DATE)", DateUtils.toString(startDate)));
        }
        if (endDate != null) {
            filters.add(String.format("s.[endDate] <= CAST('%s' AS DATE)", DateUtils.toString(endDate)));
        }
        String where = filters.stream()
                .map(f -> "(" + f + ")")
                .reduce((f1, f2) -> f1 + " AND " + f2)
                .orElse("");
        return "SELECT * FROM [nt:base] AS s WHERE " + where + " ORDER BY s.[startDate] DESC";
    }
}
