package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.util.DateUtils;
import dev.vml.es.acm.core.util.Range;
import dev.vml.es.acm.core.util.ServletUtils;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

// TODO use Text.escapeIllegalJcrChars() to escape JCR query strings
public class ExecutionQuery {

    private String path = ExecutionHistory.ROOT;

    private String executableId;

    private String userId;

    private Date startDate;

    private Date endDate;

    private List<ExecutionStatus> statuses = new LinkedList<>();

    private Range<Integer> duration;

    public static ExecutionQuery from(SlingHttpServletRequest request) {
        ExecutionQuery result = new ExecutionQuery();
        result.setExecutableId(ServletUtils.stringParam(request, "executableId"));
        result.setUserId(ServletUtils.stringParam(request, "userId"));
        result.setStartDate(DateUtils.fromString(ServletUtils.stringParam(request, "startDate")));
        result.setEndDate(DateUtils.fromString(ServletUtils.stringParam(request, "endDate")));
        result.setStatuses(ExecutionStatus.manyOf(ServletUtils.stringsParam(request, "status")));
        result.setDuration(Range.integersParse(ServletUtils.stringParam(request, "duration")));
        return result;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (!StringUtils.startsWith(path, ExecutionHistory.ROOT)) {
            throw new IllegalArgumentException(
                    String.format("Path must be a descendant of '%s'!", ExecutionHistory.ROOT));
        }
        this.path = path;
    }

    public String getExecutableId() {
        return executableId;
    }

    public void setExecutableId(String executableId) {
        this.executableId = executableId;
    }

    public String getUserId() {
        return userId;
    }

    private void setUserId(String userId) {
        this.userId = userId;
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

    public List<ExecutionStatus> getStatuses() {
        return statuses;
    }

    public void setStatus(ExecutionStatus status) {
        setStatuses(Collections.singletonList(status));
    }

    public void setStatuses(List<ExecutionStatus> status) {
        this.statuses = status;
    }

    public Range<Integer> getDuration() {
        return duration;
    }

    public void setDuration(Range<Integer> duration) {
        this.duration = duration;
    }

    protected String toSql() {
        List<String> filters = new ArrayList<>();
        filters.add(String.format(
                "s.[%s] = '%s'", JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, HistoricalExecution.RESOURCE_TYPE));
        if (path != null) {
            filters.add(String.format("ISDESCENDANTNODE(s, '%s')", path));
        }
        if (executableId != null) {
            if (StringUtils.contains(executableId, "%")) {
                filters.add(String.format("s.[executableId] LIKE '%s'", StringUtils.replace(executableId, "%", "%%")));
            } else {
                filters.add(String.format("s.[executableId] = '%s'", executableId));
            }
        }
        if (userId != null) {
            if (StringUtils.contains(userId, "%")) {
                filters.add(String.format("s.[userId] LIKE '%s'", StringUtils.replace(userId, "%", "%%")));
            } else {
                filters.add(String.format("s.[userId] = '%s'", userId));
            }
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            filters.add(statuses.stream()
                    .map(s -> String.format("s.[status] = '%s'", s))
                    .reduce((s1, s2) -> String.format("%s OR %s", s1, s2))
                    .orElse(StringUtils.EMPTY));
        }
        if (startDate != null) {
            filters.add(String.format("s.[startDate] >= CAST('%s' AS DATE)", DateUtils.toString(startDate)));
        }
        if (endDate != null) {
            filters.add(String.format("s.[endDate] <= CAST('%s' AS DATE)", DateUtils.toString(endDate)));
        }
        if (duration != null) {
            if (duration.getStart() != null) {
                filters.add(String.format("s.[duration] >= %d", duration.getStart()));
            }
            if (duration.getEnd() != null) {
                filters.add(String.format("s.[duration] <= %d", duration.getEnd()));
            }
        }
        String where = filters.stream()
                .map(f -> "(" + f + ")")
                .reduce((f1, f2) -> f1 + " AND " + f2)
                .orElse("");

        return "SELECT * FROM [nt:base] AS s WHERE " + where + " ORDER BY s.[startDate] DESC";
    }

    @Override
    public String toString() {
        return toSql();
    }
}
