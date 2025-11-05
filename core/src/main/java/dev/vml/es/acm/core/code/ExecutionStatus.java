package dev.vml.es.acm.core.code;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sling.event.jobs.Job;

public enum ExecutionStatus {
    // queued execution only statuses
    QUEUED,
    ACTIVE,
    PARSING,
    CHECKING,
    RUNNING,
    STOPPING,
    // queued & immediate execution statuses
    SKIPPED,
    STOPPED,
    ABORTED,
    FAILED,
    SUCCEEDED;

    public static List<ExecutionStatus> manyOf(List<String> names) {
        return (names != null ? names.stream() : Stream.<String>empty())
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

    public static ExecutionStatus of(Job job, Executor executor) {
        ExecutionStatus resultStatus = Optional.ofNullable(job.getResultMessage())
                .map(QueuedMessage::fromJson)
                .map(QueuedMessage::getStatus)
                .orElse(null);
        if (resultStatus != null) {
            return resultStatus;
        }

        switch (job.getJobState()) {
            case QUEUED:
                return ExecutionStatus.QUEUED;
            case ACTIVE:
                ExecutionStatus activeStatus = of(job.getProperty(ExecutionJob.ACTIVE_STATUS_PROP, String.class))
                        .orElse(null);
                if (activeStatus != null) {
                    return activeStatus;
                }
                // TODO get rid of executor.checkStatus() / not cluster-ready
                return executor.checkStatus(job.getId()).orElse(ExecutionStatus.ACTIVE);
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
