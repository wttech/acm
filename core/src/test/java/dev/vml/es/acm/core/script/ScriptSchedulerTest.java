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
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.junit.jupiter.api.Test;
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

    private static final String KEEP_CRON_UNTIL_HEALTHY = "keepCronUntilHealthy";

    private static final String BOOT_ON_INSTANCE_LEFT = "bootOnInstanceLeft";

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    private final ShutdownSafety shutdownSafety = new ShutdownSafety();

    private final JobManager jobManager = mock(JobManager.class, RETURNS_DEEP_STUBS);

    private final HealthChecker healthChecker = mock(HealthChecker.class);

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
    void shouldKeepCronSchedulesWhenInstanceStaysUnhealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(KEEP_CRON_UNTIL_HEALTHY);
        instanceStaysUnhealthy();

        scheduler.process(bootJob());

        verify(existingCronSchedule, never()).unschedule();
    }

    @Test
    void shouldRemoveCronSchedulesOnlyAfterInstanceIsHealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(KEEP_CRON_UNTIL_HEALTHY);
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(healthChecker).checkStatus();
        order.verify(existingCronSchedule).unschedule();
    }

    @Test
    void shouldRemoveCronSchedulesBeforeHealthWaitWhenToggleIsOff() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(existingCronSchedule).unschedule();
        order.verify(healthChecker).checkStatus();
    }

    @Test
    void shouldRemoveCronSchedulesWhenToggleIsOffAndInstanceStaysUnhealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        instanceStaysUnhealthy();

        scheduler.process(bootJob());

        verify(existingCronSchedule).unschedule();
    }

    @Test
    void shouldScheduleCronScriptsAgainRightAfterRemovingThem() throws Exception {
        ScriptScheduler scheduler = createScheduler(KEEP_CRON_UNTIL_HEALTHY);
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
    void shouldKeepToggleValueForWholeBootWhenConfigurationChangesDuringHealthWait() throws Exception {
        ScriptScheduler scheduler = createScheduler(KEEP_CRON_UNTIL_HEALTHY);
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.modified(shutdownSafety, bundleContext, Collections.singletonMap(KEEP_CRON_UNTIL_HEALTHY, false));
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
        ScriptScheduler scheduler = createScheduler(KEEP_CRON_UNTIL_HEALTHY);
        instanceIsHealthy();
        bootScriptCanRun();

        scheduler.process(bootJob());
        scheduler.process(bootJob());

        verify(executionQueue, times(1)).submit(any(), any());
    }

    @Test
    void shouldScheduleBootWhenInstanceLeftAndLocalIsLeader() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule).add();
    }

    @Test
    void shouldReplacePendingBootWhenInstanceLeft() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
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
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstanceOnlyJoined() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, JOINED_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstancesAreTheSame() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, OTHER_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, OTHER_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenOnlyPropertiesChanged() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(new TopologyEvent(
                TopologyEvent.Type.PROPERTIES_CHANGED,
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstanceOfAnotherClusterLeft() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();
        TopologyView viewWithOtherCluster = view(LOCAL_INSTANCE, true, LOCAL_INSTANCE);
        withOtherClusterInstance(viewWithOtherCluster);

        scheduler.handleTopologyEvent(
                instanceChanged(viewWithOtherCluster, view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenToggleIsOff() throws Exception {
        ScriptScheduler scheduler = createScheduler();
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldNotScheduleBootWhenInstanceIsNotReady() throws Exception {
        when(instanceInfo.getType()).thenReturn(InstanceType.CLOUD_CONTAINER);
        ScriptScheduler scheduler = createScheduler(repositoryWithoutCompositeNodeStore(), BOOT_ON_INSTANCE_LEFT);
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
    void shouldRemoveCronSchedulesBeforeHealthWaitWhenOnlyBootOnInstanceLeftIsOn() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(existingCronSchedule).unschedule();
        order.verify(healthChecker).checkStatus();
    }

    @Test
    void shouldNotScheduleBootWhenOnlyKeepCronUntilHealthyIsOn() throws Exception {
        ScriptScheduler scheduler = createScheduler(KEEP_CRON_UNTIL_HEALTHY);
        JobBuilder.ScheduleBuilder newBootSchedule = newBootScheduleAfterActivation();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        verify(newBootSchedule, never()).add();
    }

    @Test
    void shouldLogLeftInstancesWhenLocalIsLeader() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertEquals(
                Collections.singletonList("Instances left the cluster: [" + LEFT_INSTANCE
                        + "] - local instance is leader, booting automatic scripts"),
                leftInstancesLogs());
    }

    @Test
    void shouldLogAllLeftInstancesSortedInOneLine() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);

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
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE, LEFT_INSTANCE),
                view(LOCAL_INSTANCE, false, LEADER_INSTANCE, LOCAL_INSTANCE)));

        assertEquals(
                Collections.singletonList("Instances left the cluster: [" + LEFT_INSTANCE
                        + "] - local instance is not leader, nothing to do"),
                leftInstancesLogs());
    }

    @Test
    void shouldNotLogLeftInstancesWhenToggleIsOff() throws Exception {
        ScriptScheduler scheduler = createScheduler();

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertTrue(leftInstancesLogs().isEmpty());
    }

    @Test
    void shouldNotLogLeftInstancesWhenInstanceOnlyJoined() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);

        scheduler.handleTopologyEvent(instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE),
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, JOINED_INSTANCE)));

        assertTrue(leftInstancesLogs().isEmpty());
    }

    @Test
    void shouldNotLogLeftInstancesWhenInstanceOfAnotherClusterLeft() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        TopologyView viewWithOtherCluster = view(LOCAL_INSTANCE, true, LOCAL_INSTANCE);
        withOtherClusterInstance(viewWithOtherCluster);

        scheduler.handleTopologyEvent(
                instanceChanged(viewWithOtherCluster, view(LOCAL_INSTANCE, true, LOCAL_INSTANCE)));

        assertTrue(leftInstancesLogs().isEmpty());
    }

    @Test
    void shouldNotFailTopologyEventWhenSchedulingFails() throws Exception {
        ScriptScheduler scheduler = createScheduler(BOOT_ON_INSTANCE_LEFT);
        when(jobManager.createJob(anyString())).thenThrow(new IllegalStateException("Repository is closing"));
        TopologyEvent instanceLeft = instanceChanged(
                view(LOCAL_INSTANCE, true, LOCAL_INSTANCE, LEFT_INSTANCE), view(LOCAL_INSTANCE, true, LOCAL_INSTANCE));

        assertDoesNotThrow(() -> scheduler.handleTopologyEvent(instanceLeft));
    }

    private ScriptScheduler createScheduler(String... togglesOn) throws Exception {
        return createScheduler(repositoryWithAutomaticScriptsFolder(), togglesOn);
    }

    private ScriptScheduler createScheduler(ResourceResolverFactory resolverFactory, String... togglesOn)
            throws Exception {
        Map<String, Object> toggles = new HashMap<>();
        Arrays.stream(togglesOn).forEach(toggle -> toggles.put(toggle, true));
        MockOsgi.activate(shutdownSafety, bundleContext, toggles);

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

    private Job bootJob() {
        Job job = mock(Job.class);
        when(job.getProperty(ScriptScheduler.JOB_PROP_TYPE, String.class))
                .thenReturn(ScriptScheduler.JobType.BOOT.name());
        return job;
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
