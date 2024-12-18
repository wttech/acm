package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.util.DateUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.*;

public class HistoricalExecution implements Execution, Comparable<HistoricalExecution> {

    public static final String PRIMARY_TYPE = "nt:unstructured";

    public static final String RESOURCE_TYPE = "contentor/execution/history/entry";

    private final Executable executable;

    private final String id;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final Long duration;

    private final String error;

    private final String output;

    public HistoricalExecution(Resource resource) {
        ValueMap props = resource.getValueMap();

        this.executable = new Code(
                props.get("executableId", String.class),
                props.get("executableContent", String.class
        ));
        this.id = props.get("id", String.class);
        this.status = ExecutionStatus.of(props.get("status", String.class)).orElse(null);
        this.startDate = DateUtils.toDate(props.get("startDate", Calendar.class));
        this.endDate = DateUtils.toDate(props.get("endDate", Calendar.class));
        this.duration = props.get("duration", Long.class);
        this.error = props.get("error", String.class);
        this.output = props.get("output", String.class);
    }

    protected static Map<String, Object> toMap(ImmediateExecution execution) {
        Map<String, Object> props = new HashMap<>();

        props.put("executableId", execution.getExecutable().getId());
        props.put("executableContent", execution.getExecutable().getContent());
        props.put("id", execution.getId());
        props.put("status", execution.getStatus().name());
        props.put("startDate", DateUtils.toCalendar(execution.getStartDate()));
        props.put("endDate", DateUtils.toCalendar(execution.getEndDate()));
        props.put("duration", execution.getDuration());
        props.put("error", execution.getError());
        props.put("output", execution.readOutput());

        props.entrySet().removeIf(e -> e.getValue() == null);
        props.put(JcrConstants.JCR_PRIMARYTYPE, PRIMARY_TYPE);
        props.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, RESOURCE_TYPE);

        return props;
    }

    public static boolean check(Resource resource) {
        return Optional.ofNullable(resource)
                .filter(r -> r.isResourceType(RESOURCE_TYPE))
                .isPresent();
    }

    public static Optional<HistoricalExecution> from(Resource resource) {
        return Optional.ofNullable(resource)
                .filter(HistoricalExecution::check)
                .map(HistoricalExecution::new);
    }

    @Override
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public String getId() {
        return id;
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
    public String getOutput() {
        return output;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("executable", getExecutable())
                .append("status", getStatus())
                .append("duration", getDuration())
                .toString();
    }

    @Override
    public int compareTo(HistoricalExecution o) {
        if (o == null) {
            return 1;
        }
        return getStartDate().compareTo(o.getStartDate());
    }
}
