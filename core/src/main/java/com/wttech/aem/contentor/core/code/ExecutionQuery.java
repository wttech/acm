package com.wttech.aem.contentor.core.code;

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

    public static ExecutionQuery from(SlingHttpServletRequest request) {
        ExecutionQuery result = new ExecutionQuery();
        result.setStartDate(ServletUtils.dateParam(request, "startDate"));
        result.setEndDate(ServletUtils.dateParam(request, "endDate"));
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

    protected String toSql() {
        List<String> filters = new ArrayList<>();
        filters.add(String.format("s.[%s] = '%s'", JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, HistoricalExecution.RESOURCE_TYPE));
        if (path != null) {
            filters.add(String.format("ISDESCENDANTNODE(s, '%s')", path));
        }
        if (startDate != null) {
            filters.add(String.format("s.[startDate] >= CAST('%s' AS DATE)", startDate));
        }
        if (endDate != null) {
            filters.add(String.format("s.[endDate] <= CAST('%s' AS DATE)", endDate));
        }
        String where = filters.stream()
                .map(f -> "(" + f + ")")
                .reduce((f1, f2) -> f1 + " AND " + f2)
                .orElse("");
        return "SELECT * FROM [nt:base] AS s WHERE " + where + " ORDER BY s.[startDate] DESC";
    }
}
