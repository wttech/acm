package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ExceptionUtils;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ImmediateExecution implements Execution {

    @JsonIgnore
    private final transient ExecutionContext context;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final String error;

    public ImmediateExecution(
            ExecutionContext context, ExecutionStatus status, Date startDate, Date endDate, String error) {
        this.context = context;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.error = error;
    }

    @Override
    public String getId() {
        return context.getId();
    }

    @Override
    public String getUserId() {
        return context.getUserId();
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
        if (getStartDate() == null || getEndDate() == null) {
            return 0L;
        }
        return endDate.getTime() - startDate.getTime();
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public String getOutput() {
        context.getOutput().flush();
        try (InputStream stream = context.getOutput().read()) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getInstance() {
        return context.getCodeContext().getOsgiContext().readInstanceState();
    }

    public InputStream readOutput() throws AcmException {
        context.getOutput().flush();
        return context.getOutput().read();
    }

    @Override
    public Executable getExecutable() {
        return context.getExecutable();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("executable", getExecutable())
                .append("status", getStatus())
                .append("duration", getDuration())
                .append("error", StringUtils.abbreviate(error, 200))
                .toString();
    }

    static class Builder {

        private final ExecutionContext context;

        private Date startDate;

        private String error;

        public Builder(ExecutionContext context) {
            this.context = context;
        }

        public Builder start() {
            this.startDate = new Date();
            return this;
        }

        public Builder error(Throwable e) {
            this.error = ExceptionUtils.toString(e);
            return this;
        }

        public ImmediateExecution end(ExecutionStatus status) {
            Date endDate = new Date();
            return new ImmediateExecution(context, status, startDate, endDate, error);
        }
    }

    @Override
    public InputValues getInputs() {
        return new InputValues(context.getInputs().values());
    }

    @Override
    public OutputValues getOutputs() {
        return new OutputValues(context.getOutputs().getDefinitions().values());
    }
}
