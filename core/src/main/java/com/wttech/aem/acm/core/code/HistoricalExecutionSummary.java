package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.util.DateUtils;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

public class HistoricalExecutionSummary implements ExecutionSummary, Comparable<HistoricalExecutionSummary> {

    private final String id;

    private final String userId;

    private final String executableId;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final Long duration;

    public HistoricalExecutionSummary(Resource resource) {
        try {
            ValueMap props = resource.getValueMap();

            this.id = props.get("id", String.class);
            this.userId = props.get("userId", String.class);
            this.executableId = props.get("executableId", String.class);
            this.status = ExecutionStatus.of(props.get("status", String.class)).orElse(null);
            this.startDate = DateUtils.toDate(props.get("startDate", Calendar.class));
            this.endDate = DateUtils.toDate(props.get("endDate", Calendar.class));
            this.duration = props.get("duration", Long.class);
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot read historical execution summary from resource '%s'!", resource.getPath()),
                    e);
        }
    }

    public static boolean check(Resource resource) {
        return Optional.ofNullable(resource)
                .filter(r -> r.isResourceType(HistoricalExecution.RESOURCE_TYPE))
                .isPresent();
    }

    public static Optional<HistoricalExecutionSummary> from(Resource resource) {
        return Optional.ofNullable(resource)
                .filter(HistoricalExecutionSummary::check)
                .map(HistoricalExecutionSummary::new);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getExecutableId() {
        return executableId;
    }

    @Override
    public ExecutionStatus getStatus() {
        return status;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("executableId", getExecutableId())
                .append("status", getStatus())
                .append("duration", getDuration())
                .toString();
    }

    @Override
    public int compareTo(HistoricalExecutionSummary o) {
        if (o == null) {
            return 1;
        }
        return getStartDate().compareTo(o.getStartDate());
    }
}
