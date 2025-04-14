package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.util.DateUtils;
import com.wttech.aem.acm.core.util.JsonUtils;
import com.wttech.aem.acm.core.util.ResourceUtils;
import java.io.InputStream;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

public class HistoricalExecution implements Execution, Comparable<HistoricalExecution> {

    public static final String PRIMARY_TYPE = "nt:unstructured";

    public static final String RESOURCE_TYPE = "acm/execution/history/entry";

    private final String id;

    private final String userId;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final Long duration;

    private final String error;

    private final String output;

    private final Executable executable;

    public HistoricalExecution(Resource resource) {
        try {
            ValueMap props = resource.getValueMap();

            this.id = props.get("id", String.class);
            this.userId = props.get("userId", String.class);
            this.status = ExecutionStatus.of(props.get("status", String.class)).orElse(null);
            this.startDate = DateUtils.toDate(props.get("startDate", Calendar.class));
            this.endDate = DateUtils.toDate(props.get("endDate", Calendar.class));
            this.duration = props.get("duration", Long.class);
            this.error = props.get("error", String.class);
            this.output = props.get("output", String.class);

            this.executable = new Code(
                    props.get("executableId", String.class),
                    props.get("executableContent", String.class),
                    JsonUtils.read(props.get("executableArguments", InputStream.class), ArgumentValues.class));
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot read historical execution from resource '%s'!", resource.getPath()), e);
        }
    }

    protected static Map<String, Object> toMap(ExecutionContext context, ImmediateExecution execution) {
        try {
            Map<String, Object> props = new HashMap<>();

            props.put("id", execution.getId());
            props.put("userId", ResourceUtils.serviceOrImpersonatedUserId(context.getResourceResolver()));
            props.put("status", execution.getStatus().name());
            props.put("startDate", DateUtils.toCalendar(execution.getStartDate()));
            props.put("endDate", DateUtils.toCalendar(execution.getEndDate()));
            props.put("duration", execution.getDuration());
            props.put("error", execution.getError());
            props.put("output", execution.readOutput());

            props.put("executableId", execution.getExecutable().getId());
            props.put("executableContent", execution.getExecutable().getContent());
            props.put(
                    "executableArguments",
                    JsonUtils.writeToStream(execution.getExecutable().getArguments()));

            props.entrySet().removeIf(e -> e.getValue() == null);
            props.put(JcrConstants.JCR_PRIMARYTYPE, PRIMARY_TYPE);
            props.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, RESOURCE_TYPE);

            return props;
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot serialize historical execution to map '%s'!", execution.getId()), e);
        }
    }

    public static boolean check(Resource resource) {
        return Optional.ofNullable(resource)
                .filter(r -> r.isResourceType(RESOURCE_TYPE))
                .isPresent();
    }

    public static Optional<HistoricalExecution> from(Resource resource) {
        return Optional.ofNullable(resource).filter(HistoricalExecution::check).map(HistoricalExecution::new);
    }

    @Override
    public String getUserId() {
        return userId;
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
    public Executable getExecutable() {
        return executable;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
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
