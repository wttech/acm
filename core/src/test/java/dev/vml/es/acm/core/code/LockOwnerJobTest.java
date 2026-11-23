package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LockOwnerJobTest {

    private static final String JOB_ID = "2026/11/21/22/14/dc367ca7_4";

    private final JobManager jobManager = mock(JobManager.class);

    @Test
    void shouldSeeQueuedJobAsAwaitingDelivery() {
        jobIs(Job.JobState.QUEUED);

        assertTrue(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldSeeActiveJobAsAwaitingDelivery() {
        jobIs(Job.JobState.ACTIVE);

        assertTrue(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @ParameterizedTest
    @EnumSource(
            value = Job.JobState.class,
            names = {"SUCCEEDED", "STOPPED", "GIVEN_UP", "ERROR", "DROPPED"})
    void shouldSeeFinishedJobAsNotAwaitingDelivery(Job.JobState finishedState) {
        jobIs(finishedState);

        assertFalse(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldTreatJobThatCannotBeFoundAsAwaitingDelivery() {
        assertTrue(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldTreatFoundJobWithAnotherIdAsAwaitingDelivery() {
        Job otherJob = mock(Job.class);
        when(otherJob.getId()).thenReturn("2026/11/21/22/14/other_9");
        when(otherJob.getJobState()).thenReturn(Job.JobState.GIVEN_UP);
        when(jobManager.getJobById(JOB_ID)).thenReturn(otherJob);

        assertTrue(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldTreatJobAsAwaitingDeliveryWhenJobLookupFails() {
        when(jobManager.getJobById(JOB_ID)).thenThrow(new IllegalStateException("Repository unavailable"));

        assertTrue(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldTreatJobAsAwaitingDeliveryWhenJobStateCannotBeRead() {
        Job brokenJob = mock(Job.class);
        when(brokenJob.getId()).thenReturn(JOB_ID);
        when(brokenJob.getJobState()).thenThrow(new IllegalStateException("Unknown state"));
        when(jobManager.getJobById(JOB_ID)).thenReturn(brokenJob);

        assertTrue(new LockOwnerJob(jobManager).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldTreatJobAsAwaitingDeliveryWithoutJobManager() {
        assertTrue(new LockOwnerJob(null).isAwaitingDelivery(JOB_ID));
    }

    @Test
    void shouldSeeLockWithoutExecutionIdAsNotAwaitingDeliveryWithoutAskingSling() {
        assertFalse(new LockOwnerJob(jobManager).isAwaitingDelivery(" "));

        verifyNoInteractions(jobManager);
    }

    private void jobIs(Job.JobState state) {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(JOB_ID);
        when(job.getJobState()).thenReturn(state);
        when(jobManager.getJobById(JOB_ID)).thenReturn(job);
    }
}
