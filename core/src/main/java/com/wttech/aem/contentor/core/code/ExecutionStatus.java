package com.wttech.aem.contentor.core.code;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.sling.event.jobs.Job;

public enum ExecutionStatus {
    QUEUED,
    ACTIVE,
    SKIPPED,
    STOPPED,
    ABORTED,
    FAILED,
    SUCCEEDED;

    public static List<ExecutionStatus> manyOf(List<String> names) {
        return names.stream()
                .map(ExecutionStatus::of)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static Optional<ExecutionStatus> of(String status) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(status))
                .findFirst();
    }

    public static ExecutionStatus of(Job job) {
        ExecutionStatus status = Optional.ofNullable(job.getResultMessage())
                .map(QueuedMessage::fromJson)
                .map(QueuedMessage::getStatus)
                .orElse(null);
        if (status != null) {
            return status;
        }

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

    public static List<ExecutionStatus> completed() {
        return Arrays.asList(ExecutionStatus.SUCCEEDED, ExecutionStatus.FAILED);
    }
}
