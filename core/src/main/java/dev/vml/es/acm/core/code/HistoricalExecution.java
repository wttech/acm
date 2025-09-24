package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.DateUtils;
import dev.vml.es.acm.core.util.JsonUtils;
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

    private final String instance;

    private final Executable executable;

    private final InputValues inputs;

    private final Outputs outputs;

    public HistoricalExecution(Resource resource) {
        try {
            ValueMap props = resource.getValueMap();

            this.id = props.get("id", String.class);
            this.userId = props.get("userId", String.class);
            this.status = ExecutionStatus.of(props.get("status", String.class)).orElse(null);
            this.startDate = DateUtils.toDate(props.get("startDate", Calendar.class));
            this.endDate = DateUtils.toDate(props.get("endDate", Calendar.class));
            this.duration = props.get("duration", Long.class);
            this.output = props.get("output", String.class);
            this.error = props.get("error", String.class);
            this.inputs = JsonUtils.read(props.get("inputs", InputStream.class), InputValues.class);
            this.outputs = JsonUtils.read(props.get("outputs", InputStream.class), Outputs.class);
            this.instance = props.get("instance", String.class);

            this.executable =
                    new Code(props.get("executableId", String.class), props.get("executableContent", String.class));
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Cannot read historical execution from resource '%s'!", resource.getPath()), e);
        }
    }

    protected static Map<String, Object> toMap(ExecutionContext context, ImmediateExecution execution) {
        try {
            Map<String, Object> props = new HashMap<>();

            props.put("id", execution.getId());
            props.put("userId", context.getUserId());
            props.put("status", execution.getStatus().name());
            props.put("startDate", DateUtils.toCalendar(execution.getStartDate()));
            props.put("endDate", DateUtils.toCalendar(execution.getEndDate()));
            props.put("duration", execution.getDuration());
            props.put("output", execution.readOutput());
            props.put("error", execution.getError());
            props.put("inputs", JsonUtils.writeToStream(execution.getInputs()));
            props.put("outputs", JsonUtils.writeToStream(execution.getOutputs()));
            props.put("instance", execution.getInstance());

            props.put("executableId", execution.getExecutable().getId());
            props.put("executableContent", execution.getExecutable().getContent());

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
    public String getInstance() {
        return instance;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public InputValues getInputs() {
        return inputs;
    }

    @Override
    public Outputs getOutputs() {
        return outputs;
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
