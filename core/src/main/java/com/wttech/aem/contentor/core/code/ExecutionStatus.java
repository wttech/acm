package com.wttech.aem.contentor.core.code;

import org.apache.sling.event.jobs.Job;

public enum ExecutionStatus {
    QUEUED,
    ACTIVE,
    STOPPED,
    FAILED,
    SUCCEEDED;

    public static ExecutionStatus of(Job job, String error) {
        Job.JobState state = job.getJobState();

        ExecutionStatus result;
        if (state == Job.JobState.QUEUED) {
            result = ExecutionStatus.QUEUED;
        } else if (state == Job.JobState.ACTIVE) {
            result = ExecutionStatus.ACTIVE;
        } else if (state == Job.JobState.STOPPED) {
            result = ExecutionStatus.STOPPED;
        } else if (state == Job.JobState.SUCCEEDED) {
            if (error != null) {
                result = ExecutionStatus.FAILED;
            } else {
                result = ExecutionStatus.SUCCEEDED;
            }
        } else {
            result = ExecutionStatus.FAILED;
        }

        return result;
    }
}
