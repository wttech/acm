package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.util.ExceptionUtils;
import java.io.InputStream;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ImmediateExecution implements Execution {

    private final Executable executable;

    private final String id;

    private final ExecutionStatus status;

    private final Date startDate;

    private final Date endDate;

    private final String error;

    public ImmediateExecution(
            Executable executable, String id, ExecutionStatus status, Date startDate, Date endDate, String error) {
        this.executable = executable;
        this.id = id;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.error = error;
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
        return endDate.getTime() - startDate.getTime();
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public String getOutput() {
        return ExecutionOutput.readString(getId()).orElse(null);
    }

    public InputStream readOutput() throws ContentorException {
        return ExecutionOutput.read(getId());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
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
            return new ImmediateExecution(context.getExecutable(), context.getId(), status, startDate, endDate, error);
        }
    }
}
