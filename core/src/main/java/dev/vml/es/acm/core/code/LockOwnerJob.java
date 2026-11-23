package dev.vml.es.acm.core.code;

import java.util.EnumSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

/**
 * Tells whether Sling still holds the queued job of a lock's execution, so it will deliver the job again.
 * A job that Sling cannot find counts as held, because a job moved to another instance is found late.
 */
class LockOwnerJob {

    private static final Set<Job.JobState> FINISHED_STATES = EnumSet.of(
            Job.JobState.SUCCEEDED,
            Job.JobState.STOPPED,
            Job.JobState.GIVEN_UP,
            Job.JobState.ERROR,
            Job.JobState.DROPPED);

    private final JobManager jobManager;

    LockOwnerJob(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    /** Unknown counts as awaiting, so a lock is never taken over from a job that may still run. */
    boolean isAwaitingDelivery(String executionId) {
        if (StringUtils.isBlank(executionId)) {
            return false;
        }
        if (jobManager == null) {
            return true;
        }
        try {
            Job job = jobManager.getJobById(executionId);
            return job == null || !executionId.equals(job.getId()) || !FINISHED_STATES.contains(job.getJobState());
        } catch (RuntimeException e) {
            return true;
        }
    }
}
