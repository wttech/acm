package com.wttech.aem.contentor.core.code;

import java.util.Arrays;
import java.util.Optional;
import org.apache.sling.event.jobs.Job;

public enum ExecutionStatus {
  QUEUED,
  ACTIVE,
  STOPPED,
  FAILED,
  SKIPPED,
  ABORTED,
  SUCCEEDED;

  public static Optional<ExecutionStatus> of(String status) {
    return Arrays.stream(values()).filter(s -> s.name().equalsIgnoreCase(status)).findFirst();
  }

  public static ExecutionStatus of(Job job) {
    switch (job.getJobState()) {
      case QUEUED:
        return ExecutionStatus.QUEUED;
      case ACTIVE:
        return ExecutionStatus.ACTIVE;
      case STOPPED:
        return ExecutionStatus.STOPPED;
      case SUCCEEDED:
        return ExecutionStatus.SUCCEEDED;
      case DROPPED:
      case GIVEN_UP:
      case ERROR:
      default:
        return ExecutionStatus.FAILED;
    }
  }
}
