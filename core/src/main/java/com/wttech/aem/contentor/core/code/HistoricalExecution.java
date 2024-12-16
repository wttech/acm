package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.util.DateUtils;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

public class HistoricalExecution implements Execution {

  private final Executable executable;

  private final String id;

  private final ExecutionStatus status;

  private final Date startDate;

  private final Date endDate;

  private final Long duration;

  private final String error;

  private final String output;

  public HistoricalExecution(Resource resource) {
    ValueMap valueMap = resource.getValueMap();
    this.executable =
        new Code(
            valueMap.get("executableId", String.class),
            valueMap.get("executableContent", String.class));
    this.id = valueMap.get("id", String.class);
    this.status = ExecutionStatus.of(valueMap.get("status", String.class)).orElse(null);
    this.startDate = DateUtils.toDate(valueMap.get("startDate", Calendar.class));
    this.endDate = DateUtils.toDate(valueMap.get("endDate", Calendar.class));
    this.duration = valueMap.get("duration", Long.class);
    this.error = valueMap.get("error", String.class);
    this.output = valueMap.get("output", String.class);
  }

  protected static Map<String, Object> toMap(Execution execution) {
    Map<String, Object> valueMap = new HashMap<>();
    valueMap.put("executableId", execution.getExecutable().getId());
    valueMap.put("executableContent", execution.getExecutable().getContent());
    valueMap.put("id", execution.getId());
    valueMap.put("status", execution.getStatus().name());
    valueMap.put("startDate", DateUtils.toCalendar(execution.getStartDate()));
    valueMap.put("endDate", DateUtils.toCalendar(execution.getEndDate()));
    valueMap.put("duration", execution.getDuration());
    valueMap.put("error", execution.getError());
    valueMap.put("output", execution.getOutput());
    return valueMap;
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
}
