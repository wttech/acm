package dev.vml.es.acm.core.script;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.code.Execution;
import dev.vml.es.acm.core.code.ExecutionContext;
import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.code.ExecutionStatus;
import dev.vml.es.acm.core.code.Executor;
import dev.vml.es.acm.core.code.ScheduleResult;
import dev.vml.es.acm.core.code.schedule.BootSchedule;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthIssue;
import dev.vml.es.acm.core.instance.HealthIssueCategory;
import dev.vml.es.acm.core.instance.HealthIssueSeverity;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.instance.TaggedHealthChecks;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.slf4j.helpers.MessageFormatter;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ScriptSchedulerTest {

    private static final String CRON_EXPRESSION = "0 0 10 * * ?";

    private static final String LOCAL_INSTANCE = "local-instance";

    private static final String LEFT_INSTANCE = "left-instance";

    private static final String JOINED_INSTANCE = "joined-instance";

    private static final String LEADER_INSTANCE = "leader-instance";

    private static final String OTHER_INSTANCE = "other-instance";

    private static final String OTHER_CLUSTER_INSTANCE = "other-cluster-instance";

    private static final String ENABLED = "enabled";

    private static final String SHUTDOWN_MARKER_FILE = "shutdownMarkerFile";

    @TempDir
    Path tempDir;

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    private final ShutdownSafety shutdownSafety = new ShutdownSafety();

    private final JobManager jobManager = mock(JobManager.class, RETURNS_DEEP_STUBS);

    private final HealthChecker healthChecker = mock(HealthChecker.class);

    private final TaggedHealthChecks taggedHealthChecks = mock(TaggedHealthChecks.class);

    private final ScheduledJobInfo existingCronSchedule = mock(ScheduledJobInfo.class);

    private final ScheduledJobInfo pendingBootSchedule = mock(ScheduledJobInfo.class);

    private final InstanceInfo instanceInfo = mock(InstanceInfo.class);

    private final ExecutionQueue executionQueue = mock(ExecutionQueue.class);

    private final Executor executor = mock(Executor.class);

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    @AfterEach
    void clearLogs() {
        TestLoggerFactory.clear();
    }

    @Test
    void shouldNotQueueBootScriptsWhileTaggedHealthCheckIsCritical() throws Exception {
        taggedHealthCheckIsCritical();
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue, never()).submit(any(), any());
    }

    @Test
    void shouldQueueBootScriptsWhenNoTaggedHealthCheckIsCritical() throws Exception {
        bundleContext.registerService(TaggedHealthChecks.class, taggedHealthChecks, null);
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldQueueBootScriptsWhenTaggedHealthChecksAreNotAvailable() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldIgnoreTaggedHealthChecksWhenShutdownSafetyIsDisabled() throws Exception {
        taggedHealthCheckIsCritical();
        ScriptScheduler scheduler = createScheduler();
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
        verifyNoInteractions(taggedHealthChecks);
    }

    @Test
    void shouldQueueCronScriptWhileTaggedHealthCheckIsCritical() throws Exception {
        taggedHealthCheckIsCritical();
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(cronJob(ScriptType.AUTOMATIC.root() + "/boot" + Script.EXTENSION));

        verify(executionQueue).submit(any(), any());
        verifyNoInteractions(taggedHealthChecks);
    }

    @Test
    void shouldNotCheckHealthWhenSchedulerIsAlreadyDeactivatedAndShutdownSafetyIsEnabled() throws Exception {
        ScriptScheduler deactivatedScheduler = createScheduler(ENABLED);
        MockOsgi.deactivate(deactivatedScheduler, bundleContext);

        deactivatedScheduler.process(bootJob());

        verify(healthChecker, never()).checkStatus();
    }

    @Test
    void shouldNotQueueBootScriptsWhenTaggedHealthChecksCannotBeRead() throws Exception {
        when(taggedHealthChecks.findCriticalIssues(any())).thenThrow(new IllegalStateException("Executor stopping"));
        bundleContext.registerService(TaggedHealthChecks.class, taggedHealthChecks, null);
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue, never()).submit(any(), any());
    }

    @Test
    void shouldNotGateBootWhenHealthCheckTagsAreEmpty() throws Exception {
        taggedHealthCheckIsCritical();
        Map<String, Object> gateWithoutTags = new HashMap<>();
        gateWithoutTags.put(ENABLED, true);
        gateWithoutTags.put("healthCheckTags", new String[0]);
        ScriptScheduler scheduler = createScheduler();
        MockOsgi.modified(shutdownSafety, bundleContext, gateWithoutTags);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
        verifyNoInteractions(taggedHealthChecks);
    }

    @Test
    void shouldLogMissingHealthCheckServiceOnce() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();

        scheduler.process(bootJob());
        scheduler.process(bootJob());

        assertEquals(
                1,
                infoLogs("Health check wait on boot has no effect: no health check service")
                        .size());
    }

    @Test
    void shouldStopWaitingForHealthyInstanceWhenSchedulerIsDeactivatedAndShutdownSafetyIsEnabled() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        MockOsgi.modified(scheduler, bundleContext, bootHealthWait(5L));
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.deactivate(scheduler, bundleContext);
            return new HealthStatus(Collections.emptyList(), false);
        });

        scheduler.process(bootJob());

        verify(healthChecker, times(1)).checkStatus();
    }

    @Test
    void shouldNotQueueBootScriptsWhenSchedulerIsDeactivatedDuringHealthyCheckAndShutdownSafetyIsEnabled()
            throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        bootScriptCanRun();
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.deactivate(scheduler, bundleContext);
            return new HealthStatus(Collections.emptyList(), true);
        });

        scheduler.process(bootJob());

        verify(executionQueue, never()).submit(any(), any());
    }

    @Test
    void shouldKeepWaitingForHealthyInstanceAfterDeactivationWhenShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        MockOsgi.modified(scheduler, bundleContext, bootHealthWait(5L));
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.deactivate(scheduler, bundleContext);
            return new HealthStatus(Collections.emptyList(), false);
        });

        scheduler.process(bootJob());

        verify(healthChecker, times(5)).checkStatus();
    }

    @Test
    void shouldQueueBootScriptsAfterDeactivationDuringHealthyCheckWhenShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        bootScriptCanRun();
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.deactivate(scheduler, bundleContext);
            return new HealthStatus(Collections.emptyList(), true);
        });

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldCheckHealthAfterDeactivationWhenShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler deactivatedScheduler = createScheduler();
        MockOsgi.deactivate(deactivatedScheduler, bundleContext);
        instanceIsHealthy();

        deactivatedScheduler.process(bootJob());

        verify(healthChecker).checkStatus();
    }

    @Test
    void shouldKeepWaitingForHealthyInstanceAfterDeactivationOnCronRun() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        MockOsgi.modified(scheduler, bundleContext, cronHealthWait(3L));
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.deactivate(scheduler, bundleContext);
            return new HealthStatus(Collections.emptyList(), false);
        });

        scheduler.process(cronJob(ScriptType.AUTOMATIC.root() + "/cron" + Script.EXTENSION));

        verify(healthChecker, times(3)).checkStatus();
    }

    @Test
    void shouldKeepStopWhenDeactivatedForWholeBootWhenConfigurationChangesDuringHealthWait() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        MockOsgi.modified(scheduler, bundleContext, bootHealthWait(5L));
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.modified(shutdownSafety, bundleContext, Collections.emptyMap());
            MockOsgi.deactivate(scheduler, bundleContext);
            return new HealthStatus(Collections.emptyList(), false);
        });

        scheduler.process(bootJob());

        verify(healthChecker, times(1)).checkStatus();
    }

    @Test
    void shouldStopWaitingForHealthyInstanceWhenBootIsInterruptedAndShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        instanceStaysUnhealthy();

        boolean interruptKept = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread.currentThread().interrupt();
            scheduler.process(bootJob());
            return Thread.interrupted();
        });

        assertTrue(interruptKept);
    }

    @Test
    void shouldStopWaitingForHealthyInstanceWhenBootIsInterrupted() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceStaysUnhealthy();

        boolean interruptKept = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread.currentThread().interrupt();
            scheduler.process(bootJob());
            return Thread.interrupted();
        });

        assertTrue(interruptKept);
    }

    @Test
    void shouldKeepCronSchedulesWhenBootIsInterrupted() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceStaysUnhealthy();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread.currentThread().interrupt();
            scheduler.process(bootJob());
            Thread.interrupted();
        });

        verify(existingCronSchedule, never()).unschedule();
    }

    @Test
    void shouldKeepCronSchedulesWhenInstanceStaysUnhealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceStaysUnhealthy();

        scheduler.process(bootJob());

        verify(existingCronSchedule, never()).unschedule();
    }

    @Test
    void shouldRemoveCronSchedulesOnlyAfterInstanceIsHealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(healthChecker).checkStatus();
        order.verify(existingCronSchedule).unschedule();
    }

    @Test
    void shouldRemoveCronSchedulesBeforeHealthWaitWhenShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(existingCronSchedule).unschedule();
        order.verify(healthChecker).checkStatus();
    }

    @Test
    void shouldRemoveCronSchedulesWhenShutdownSafetyIsDisabledAndInstanceStaysUnhealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        instanceStaysUnhealthy();

        scheduler.process(bootJob());

        verify(existingCronSchedule).unschedule();
    }

    @Test
    void shouldScheduleCronScriptsAgainRightAfterRemovingThem() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        cronScriptExists();

        scheduler.process(bootJob());

        JobBuilder.ScheduleBuilder newSchedule =
                jobManager.createJob(ScriptScheduler.JOB_TOPIC).schedule();
        InOrder order = inOrder(existingCronSchedule, newSchedule);
        order.verify(existingCronSchedule).unschedule();
        order.verify(newSchedule).cron(CRON_EXPRESSION);
        order.verify(newSchedule).add();
    }

    @Test
    void shouldKeepEnabledValueForWholeBootWhenConfigurationChangesDuringHealthWait() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.modified(shutdownSafety, bundleContext, Collections.singletonMap(ENABLED, false));
            return new HealthStatus(Collections.emptyList(), true);
        });

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(healthChecker).checkStatus();
        order.verify(existingCronSchedule).unschedule();
        verify(existingCronSchedule, times(1)).unschedule();
    }

    @Test
    void shouldQueueBootScriptOnFirstBootAndSkipItOnTheNextOne() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());
        scheduler.process(bootJob());

        verify(executionQueue, times(1)).submit(any(), any());
    }

    @Test
    void shouldScheduleBootAfterInstanceLeftOnlyOnceSlingMovedTheJobsOfThatInstance() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();
        long instanceLeftAt = System.currentTimeMillis();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertTrue(scheduledBootTime(newBootSchedule) >= instanceLeftAt + 30_000L);
    }

    @Test
    void shouldScheduleBootAfterInstanceLeftAfterLongerBootDelay() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        MockOsgi.modified(scheduler, bundleContext, Collections.singletonMap("bootDelay", 60_000L));
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();
        long instanceLeftAt = System.currentTimeMillis();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertTrue(scheduledBootTime(newBootSchedule) >= instanceLeftAt + 60_000L);
    }

    @Test
    void shouldScheduleBootOnDemandAfterBootDelay() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();
        long requestedAt = System.currentTimeMillis();

        scheduler.bootOnDemand();

        assertTrue(scheduledBootTime(newBootSchedule) < requestedAt + 30_000L);
    }

    @Test
    void shouldScheduleBootWhenInstanceLeftAndLocalIsLeader() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule).add();
    }

    @Test
    void shouldIgnoreInstanceLeftAfterDeactivation() throws Exception {
        ScriptScheduler deactivatedScheduler = createScheduler(ENABLED);
        MockOsgi.deactivate(deactivatedScheduler, bundleContext);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        deactivatedScheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldReplacePendingBootWhenInstanceLeft() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();
        clearInvocations(pendingBootSchedule);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        InOrder order = inOrder(pendingBootSchedule, newBootSchedule);
        order.verify(pendingBootSchedule).unschedule();
        order.verify(newBootSchedule).add();
    }

    @Test
    void shouldNotScheduleBootWhenLocalIsNotLeader() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstanceOnlyJoined() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, JOINED_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstancesAreTheSame() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, OTHER_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, OTHER_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenOnlyPropertiesChanged() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(new TopologyEvent(
                TopologyEvent.Type.PROPERTIES_CHANGED,
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstanceOfAnotherClusterLeft() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();
        TopologyView viewWithOtherCluster = view(LOCAL_INSTANCE, true, LOCAL_INSTANCE);
        withOtherClusterInstance(viewWithOtherCluster);

        scheduler.handleTopologyEvent(
                instanceChanged(viewWithOtherCluster, view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstanceIsNotReady() throws Exception {
        when(instanceInfo.getType()).thenReturn(InstanceType.CLOUD_CONTAINER);
        ScriptScheduler scheduler = createScheduler(repositoryWithoutCompositeNodeStore(), ENABLED);
        JobBuilder.ScheduleBuilder newBootSchedule =
                jobManager.createJob(ScriptScheduler.JOB_TOPIC).schedule();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
        assertEquals(
                Collections.singletonList("Instances left the cluster: [" + LEFT_INSTANCE
                        + "] - local instance is leader but not ready, nothing to do"),
                leftInstancesLogs());
    }

    @Test
    void shouldLogLeftInstancesWhenLocalIsLeader() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertEquals(
                Collections.singletonList("Instances left the cluster: [" + LEFT_INSTANCE
                        + "] - local instance is leader, booting automatic scripts"),
                leftInstancesLogs());
    }

    @Test
    void shouldLogAllLeftInstancesSortedInOneLine() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, OTHER_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertEquals(
                Collections.singletonList("Instances left the cluster: [" + LEFT_INSTANCE + ", " + OTHER_INSTANCE
                        + "] - local instance is leader, booting automatic scripts"),
                leftInstancesLogs());
    }

    @Test
    void shouldLogLeftInstancesWhenLocalIsNotLeader() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE)));

        assertEquals(
                Collections.singletonList("Instances left the cluster: [" + LEFT_INSTANCE
                        + "] - local instance is not leader, nothing to do"),
                leftInstancesLogs());
    }

    @Test
    void shouldNotLogLeftInstancesWhenShutdownSafetyIsDisabled() throws Exception {
        ScriptScheduler scheduler = createScheduler();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertTrue(leftInstancesLogs().isEmpty());
    }

    @Test
    void shouldNotLogLeftInstancesWhenInstanceOnlyJoined() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, JOINED_INSTANCE)));

        assertTrue(leftInstancesLogs().isEmpty());
    }

    @Test
    void shouldNotLogLeftInstancesWhenInstanceOfAnotherClusterLeft() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        TopologyView viewWithOtherCluster = view(LOCAL_INSTANCE, true, LOCAL_INSTANCE);
        withOtherClusterInstance(viewWithOtherCluster);

        scheduler.handleTopologyEvent(
                instanceChanged(viewWithOtherCluster, view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertTrue(leftInstancesLogs().isEmpty());
    }

    @Test
    void shouldNotQueueBootScriptsWhileShutdownMarkerFileExists() throws Exception {
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue, never()).submit(any(), any());
    }

    @Test
    void shouldQueueBootScriptsWhenShutdownMarkerFileIsMissing() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(tempDir.resolve("shutdown").toString());
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldQueueBootScriptsWhenShutdownMarkerFolderIsMissing() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(tempDir.resolve("run/aem/shutdown").toString());
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldNotWatchShutdownMarkerFileWhenItIsNotSet() throws Exception {
        existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldNotQueueBootScriptsWhileShutdownMarkerFileExistsAndHealthCheckTagsAreEmpty() throws Exception {
        bundleContext.registerService(TaggedHealthChecks.class, taggedHealthChecks, null);
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        Map<String, Object> markerWithoutTags = new HashMap<>();
        markerWithoutTags.put(ENABLED, true);
        markerWithoutTags.put("healthCheckTags", new String[0]);
        markerWithoutTags.put(SHUTDOWN_MARKER_FILE, marker.toString());
        MockOsgi.modified(shutdownSafety, bundleContext, markerWithoutTags);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue, never()).submit(any(), any());
    }

    @Test
    void shouldNotCheckInstanceHealthWhileShutdownMarkerFileExists() throws Exception {
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());

        scheduler.process(bootJob());

        verify(healthChecker, never()).checkStatus();
    }

    @Test
    void shouldQueueCronScriptWhileShutdownMarkerFileExists() throws Exception {
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(cronJob(ScriptType.AUTOMATIC.root() + "/boot" + Script.EXTENSION));

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldQueueBootScriptsAgainOnceShutdownMarkerFileIsRemoved() throws Exception {
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());
        instanceIsHealthy();
        bootScriptCanRun();
        scheduler.process(bootJob());

        Files.delete(marker);
        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldKeepCronSchedulesWhileShutdownMarkerFileExists() throws Exception {
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());

        scheduler.process(bootJob());

        verify(existingCronSchedule, never()).unschedule();
    }

    @Test
    void shouldNameShutdownMarkerFileWhenBootIsPaused() throws Exception {
        Path marker = existingShutdownMarkerFile();
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());

        scheduler.process(bootJob());

        assertFalse(warnLogs("Automatic scripts queueing and scheduling paused").isEmpty());
        assertTrue(warnLogs("Automatic scripts queueing and scheduling paused").stream()
                .allMatch(message -> message.contains("Shutdown marker file exists: " + marker)));
    }

    @Test
    void shouldNotQueueBootScriptWhenShutdownMarkerFileAppearsAfterHealthCheck() throws Exception {
        Path marker = tempDir.resolve("shutdown");
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());
        instanceIsHealthy();
        bootScriptCanRun();
        Execution succeeded = executor.check(null);
        when(executor.check(any())).thenAnswer(invocation -> {
            if (!Files.exists(marker)) {
                Files.createFile(marker);
            }
            return succeeded;
        });

        scheduler.process(bootJob());

        verify(executionQueue, never()).submit(any(), any());
    }

    @Test
    void shouldWarnOnceWhenShutdownMarkerFileStopsBootScriptBeforeQueue() throws Exception {
        Path marker = tempDir.resolve("shutdown");
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());
        instanceIsHealthy();
        bootScriptCanRun();
        Execution succeeded = executor.check(null);
        when(executor.check(any())).thenAnswer(invocation -> {
            if (!Files.exists(marker)) {
                Files.createFile(marker);
            }
            return succeeded;
        });

        scheduler.process(bootJob());

        assertEquals(
                1,
                warnLogs("Boot script '" + ScriptType.AUTOMATIC.root() + "/boot" + Script.EXTENSION
                                + "' not queued - Shutdown marker file exists: " + marker)
                        .size());
    }

    @Test
    void shouldQueueBootScriptOnNextBootWhenShutdownMarkerFileStoppedItBeforeQueue() throws Exception {
        Path marker = tempDir.resolve("shutdown");
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(marker.toString());
        instanceIsHealthy();
        bootScriptCanRun();
        Execution succeeded = executor.check(null);
        when(executor.check(any())).thenAnswer(invocation -> {
            Files.createFile(marker);
            return succeeded;
        });
        scheduler.process(bootJob());

        Files.delete(marker);
        doReturn(succeeded).when(executor).check(any());
        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
    }

    @Test
    void shouldQueueBootScriptsAndWarnOnceWhenShutdownMarkerFileCannotBeChecked() throws Exception {
        Assumptions.assumeFalse(File.separatorChar == '\\', "POSIX permissions only");
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")), "root reads every folder");
        Path closedFolder = Files.createDirectory(tempDir.resolve("closed"));
        Path markerInClosedFolder = closedFolder.resolve("shutdown");
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(markerInClosedFolder.toString());
        instanceIsHealthy();
        bootScriptCanRun();
        closedFolder.toFile().setExecutable(false, false);
        try {
            scheduler.process(bootJob());
        } finally {
            closedFolder.toFile().setExecutable(true, false);
        }

        verify(executionQueue).submit(any(), any());
        assertEquals(
                Collections.singletonList("Shutdown marker file cannot be checked: " + markerInClosedFolder),
                warnLogs("Shutdown marker file"));
    }

    @Test
    void shouldQueueBootScriptsAndWarnOnceWhenShutdownMarkerFilePathIsInvalid() throws Exception {
        String invalidMarkerPath = "/var/run/aem/shut\u0000down";
        ScriptScheduler scheduler = createScheduler();
        watchShutdownMarkerFile(invalidMarkerPath);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());

        verify(executionQueue).submit(any(), any());
        assertEquals(
                Collections.singletonList("Shutdown marker file cannot be checked: " + invalidMarkerPath),
                warnLogs("Shutdown marker file"));
    }

    @Test
    void shouldNotFailTopologyEventWhenSchedulingFails() throws Exception {
        ScriptScheduler scheduler = createScheduler(ENABLED);
        when(jobManager.createJob(anyString())).thenThrow(new IllegalStateException("Repository is closing"));
        TopologyEvent instanceLeft = instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE));

        assertDoesNotThrow(() -> scheduler.handleTopologyEvent(instanceLeft));
    }

    private ScriptScheduler createScheduler(String... enabledSettings) throws Exception {
        return createScheduler(repositoryWithAutomaticScriptsFolder(), enabledSettings);
    }

    private ScriptScheduler createScheduler(ResourceResolverFactory resolverFactory, String... enabledSettings)
            throws Exception {
        Map<String, Object> shutdownSafetyConfig = new HashMap<>();
        Arrays.stream(enabledSettings).forEach(setting -> shutdownSafetyConfig.put(setting, true));
        MockOsgi.activate(shutdownSafety, bundleContext, shutdownSafetyConfig);

        bundleContext.registerService(ResourceResolverFactory.class, resolverFactory, null);
        bundleContext.registerService(InstanceInfo.class, instanceInfo, null);
        bundleContext.registerService(Executor.class, executor, null);
        bundleContext.registerService(JobManager.class, jobManagerWithPendingSchedules(), null);
        bundleContext.registerService(HealthChecker.class, healthChecker, null);
        bundleContext.registerService(ExecutionQueue.class, executionQueue, null);
        bundleContext.registerService(ShutdownSafety.class, shutdownSafety, null);

        Map<String, Object> config = new HashMap<>();
        config.put("healthCheckRetryCountBoot", 1L);
        config.put("healthCheckRetryInterval", 1L);

        ScriptScheduler scheduler = new ScriptScheduler();
        MockOsgi.injectServices(scheduler, bundleContext);
        MockOsgi.activate(scheduler, bundleContext, config);
        return scheduler;
    }

    /** Without scripts in it, a successful boot has nothing to queue or schedule. */
    private ResourceResolverFactory repositoryWithAutomaticScriptsFolder() throws Exception {
        try (ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap())) {
            ResourceUtil.getOrCreateResource(
                    resolver, ScriptType.AUTOMATIC.root(), "nt:unstructured", "nt:unstructured", true);
        }
        return repository;
    }

    /** Boot schedule builder of the job manager, with the boot planned on activation already cleared. */
    private JobBuilder.ScheduleBuilder newBootScheduleAfterActivation() {
        JobBuilder.ScheduleBuilder newBootSchedule =
                jobManager.createJob(ScriptScheduler.JOB_TOPIC).schedule();
        clearInvocations(newBootSchedule);
        return newBootSchedule;
    }

    private long scheduledBootTime(JobBuilder.ScheduleBuilder bootSchedule) {
        ArgumentCaptor<Date> bootTime = ArgumentCaptor.forClass(Date.class);
        verify(bootSchedule).at(bootTime.capture());
        return bootTime.getValue().getTime();
    }

    /** A cloud container whose repository is not a composite node store yet is not ready for booting. */
    private ResourceResolverFactory repositoryWithoutCompositeNodeStore() throws Exception {
        Session session = mock(Session.class);
        when(session.getNode("/apps")).thenReturn(mock(Node.class));
        when(session.hasPermission(anyString(), anyString())).thenReturn(true);
        when(session.hasCapability(anyString(), any(), any())).thenReturn(true);
        ResourceResolver resolver =
                spy(repositoryWithAutomaticScriptsFolder().getServiceResourceResolver(Collections.emptyMap()));
        doReturn(session).when(resolver).adaptTo(Session.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        when(resolverFactory.getServiceResourceResolver(any())).thenReturn(resolver);
        return resolverFactory;
    }

    private void bootScriptCanRun() throws Exception {
        String scriptPath = ScriptType.AUTOMATIC.root() + "/boot" + Script.EXTENSION;
        try (ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap())) {
            ResourceUtil.getOrCreateResource(resolver, scriptPath, "nt:file", "nt:unstructured", false);
            Map<String, Object> content = new HashMap<>();
            content.put("jcr:primaryType", "nt:resource");
            content.put(
                    "jcr:data", new ByteArrayInputStream("boolean canRun() { true }".getBytes(StandardCharsets.UTF_8)));
            resolver.create(resolver.getResource(scriptPath), "jcr:content", content);
            resolver.commit();
        }
        Execution succeeded = mock(Execution.class);
        when(succeeded.getStatus()).thenReturn(ExecutionStatus.SUCCEEDED);
        ExecutionContext parseContext = mock(ExecutionContext.class, RETURNS_DEEP_STUBS);
        when(executor.createContext(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(parseContext);
        when(executor.schedule(any())).thenReturn(new ScheduleResult(succeeded, new BootSchedule()));
        when(executor.check(any())).thenReturn(succeeded);
        when(executionQueue.getMaxSize()).thenReturn(10L);
        when(executionQueue.findByExecutableId(any())).thenReturn(Optional.empty());
    }

    private TopologyEvent instanceChanged(TopologyView viewBefore, TopologyView viewAfter) {
        return new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED, viewBefore, viewAfter);
    }

    /** Topology view with the given instances in the local cluster. The local instance can be the leader. */
    private TopologyView view(String localSlingId, boolean localIsLeader, String... clusterSlingIds) {
        ClusterView cluster = mock(ClusterView.class);
        List<InstanceDescription> instances = Arrays.stream(clusterSlingIds)
                .map(slingId -> instance(slingId, cluster))
                .collect(Collectors.toList());
        when(cluster.getInstances()).thenReturn(instances);
        InstanceDescription local = instance(localSlingId, cluster);
        when(local.isLeader()).thenReturn(localIsLeader);
        TopologyView view = mock(TopologyView.class);
        when(view.getLocalInstance()).thenReturn(local);
        when(view.getInstances()).thenReturn(new HashSet<>(instances));
        return view;
    }

    private void withOtherClusterInstance(TopologyView view) {
        Set<InstanceDescription> instances = new HashSet<>(view.getInstances());
        instances.add(instance(OTHER_CLUSTER_INSTANCE, mock(ClusterView.class)));
        when(view.getInstances()).thenReturn(instances);
    }

    private InstanceDescription instance(String slingId, ClusterView cluster) {
        InstanceDescription instance = mock(InstanceDescription.class);
        when(instance.getSlingId()).thenReturn(slingId);
        when(instance.getClusterView()).thenReturn(cluster);
        return instance;
    }

    private void cronScriptExists() throws Exception {
        try (ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap())) {
            ResourceUtil.getOrCreateResource(
                    resolver,
                    ScriptType.AUTOMATIC.root() + "/cron" + Script.EXTENSION,
                    "nt:file",
                    "nt:unstructured",
                    true);
        }
        Execution parsed = mock(Execution.class);
        when(parsed.getStatus()).thenReturn(ExecutionStatus.SUCCEEDED);
        when(executor.schedule(any())).thenReturn(new ScheduleResult(parsed, new CronSchedule(CRON_EXPRESSION)));
    }

    @SuppressWarnings("unchecked")
    private JobManager jobManagerWithPendingSchedules() {
        when(jobManager.getScheduledJobs(eq(ScriptScheduler.JOB_TOPIC), eq(-1L), any()))
                .thenAnswer(invocation -> {
                    Map<String, Object> filter = invocation.getArgument(2);
                    boolean cron =
                            ScriptScheduler.JobType.CRON.name().equals(filter.get(ScriptScheduler.JOB_PROP_TYPE));
                    Collection<ScheduledJobInfo> found =
                            Collections.singletonList(cron ? existingCronSchedule : pendingBootSchedule);
                    return found;
                });
        return jobManager;
    }

    private void instanceIsHealthy() {
        when(healthChecker.checkStatus()).thenReturn(new HealthStatus(Collections.emptyList(), true));
    }

    private void instanceStaysUnhealthy() {
        when(healthChecker.checkStatus()).thenReturn(new HealthStatus(Collections.emptyList(), false));
    }

    private void taggedHealthCheckIsCritical() {
        when(taggedHealthChecks.findCriticalIssues(any()))
                .thenReturn(Collections.singletonList(new HealthIssue(
                        HealthIssueSeverity.CRITICAL, HealthIssueCategory.INSTANCE, "Required paths check", null)));
        bundleContext.registerService(TaggedHealthChecks.class, taggedHealthChecks, null);
    }

    private Map<String, Object> bootHealthWait(long retryCount) {
        Map<String, Object> config = new HashMap<>();
        config.put("healthCheckRetryCountBoot", retryCount);
        config.put("healthCheckRetryInterval", 1L);
        return config;
    }

    private Map<String, Object> cronHealthWait(long retryCount) {
        Map<String, Object> config = new HashMap<>();
        config.put("healthCheckRetryCountCron", retryCount);
        config.put("healthCheckRetryInterval", 1L);
        return config;
    }

    private Job cronJob(String scriptPath) {
        Job job = mock(Job.class);
        when(job.getProperty(ScriptScheduler.JOB_PROP_TYPE, String.class))
                .thenReturn(ScriptScheduler.JobType.CRON.name());
        when(job.getProperty(ScriptScheduler.JOB_PROP_SCRIPT_PATH, String.class))
                .thenReturn(scriptPath);
        return job;
    }

    private Job bootJob() {
        Job job = mock(Job.class);
        when(job.getProperty(ScriptScheduler.JOB_PROP_TYPE, String.class))
                .thenReturn(ScriptScheduler.JobType.BOOT.name());
        return job;
    }

    private Path existingShutdownMarkerFile() throws Exception {
        return Files.createFile(tempDir.resolve("shutdown"));
    }

    private void watchShutdownMarkerFile(String path) {
        Map<String, Object> enabledWithMarker = new HashMap<>();
        enabledWithMarker.put(ENABLED, true);
        enabledWithMarker.put(SHUTDOWN_MARKER_FILE, path);
        MockOsgi.modified(shutdownSafety, bundleContext, enabledWithMarker);
    }

    private List<String> warnLogs(String prefix) {
        return TestLoggerFactory.getTestLogger(ScriptScheduler.class).getLoggingEvents().stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(event -> MessageFormatter.arrayFormat(
                                event.getMessage(), event.getArguments().toArray())
                        .getMessage())
                .filter(message -> message.startsWith(prefix))
                .collect(Collectors.toList());
    }

    private List<String> infoLogs(String prefix) {
        return TestLoggerFactory.getTestLogger(ScriptScheduler.class).getLoggingEvents().stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(event -> MessageFormatter.arrayFormat(
                                event.getMessage(), event.getArguments().toArray())
                        .getMessage())
                .filter(message -> message.startsWith(prefix))
                .collect(Collectors.toList());
    }

    /** INFO lines about instances that left the cluster, formatted as they appear in the log. */
    private List<String> leftInstancesLogs() {
        return TestLoggerFactory.getTestLogger(ScriptScheduler.class).getLoggingEvents().stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(event -> MessageFormatter.arrayFormat(
                                event.getMessage(), event.getArguments().toArray())
                        .getMessage())
                .filter(message -> message.startsWith("Instances left the cluster"))
                .collect(Collectors.toList());
    }
}
