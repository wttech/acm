package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.ExceptionUtils;
import java.io.InputStream;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ImmediateExecution implements Execution {

    private final Executable executable;

    private final String id;

    private final String userId;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final String error;

    public ImmediateExecution(
            Executable executable,
            String id,
            String userId,
            ExecutionStatus status,
            Date startDate,
            Date endDate,
            String error) {
        this.executable = executable;
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.error = error;
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
        return new OutputFile(getId()).readString().orElse(null);
    }

    public InputStream readOutput() throws AcmException {
        return new OutputFile(getId()).read();
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
            return new ImmediateExecution(
                    context.getExecutable(),
                    context.getId(),
                    context.getCodeContext().getResourceResolver().getUserID(),
                    status,
                    startDate,
                    endDate,
                    error);
        }
    }
}
