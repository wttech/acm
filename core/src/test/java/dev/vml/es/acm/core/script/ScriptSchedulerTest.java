package dev.vml.es.acm.core.script;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.code.Execution;
import dev.vml.es.acm.core.code.ExecutionQueue;
import dev.vml.es.acm.core.code.ExecutionStatus;
import dev.vml.es.acm.core.code.Executor;
import dev.vml.es.acm.core.code.ScheduleResult;
import dev.vml.es.acm.core.code.schedule.CronSchedule;
import dev.vml.es.acm.core.instance.HealthChecker;
import dev.vml.es.acm.core.instance.HealthStatus;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;

class ScriptSchedulerTest {

    private static final String CRON_EXPRESSION = "0 0 10 * * ?";

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    private final ShutdownSafety shutdownSafety = new ShutdownSafety();

    private final JobManager jobManager = mock(JobManager.class, RETURNS_DEEP_STUBS);

    private final HealthChecker healthChecker = mock(HealthChecker.class);

    private final ScheduledJobInfo existingCronSchedule = mock(ScheduledJobInfo.class);

    private final Executor executor = mock(Executor.class);

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    @Test
    void shouldKeepCronSchedulesWhenInstanceStaysUnhealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(true);
        instanceStaysUnhealthy();

        scheduler.process(bootJob());

        verify(existingCronSchedule, never()).unschedule();
    }

    @Test
    void shouldRemoveCronSchedulesOnlyAfterInstanceIsHealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(true);
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(healthChecker).checkStatus();
        order.verify(existingCronSchedule).unschedule();
    }

    @Test
    void shouldRemoveCronSchedulesBeforeHealthWaitWhenSwitchIsOff() throws Exception {
        ScriptScheduler scheduler = createScheduler(false);
        instanceIsHealthy();

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(existingCronSchedule).unschedule();
        order.verify(healthChecker).checkStatus();
    }

    @Test
    void shouldRemoveCronSchedulesWhenSwitchIsOffAndInstanceStaysUnhealthy() throws Exception {
        ScriptScheduler scheduler = createScheduler(false);
        instanceStaysUnhealthy();

        scheduler.process(bootJob());

        verify(existingCronSchedule).unschedule();
    }

    @Test
    void shouldScheduleCronScriptsAgainRightAfterRemovingThem() throws Exception {
        ScriptScheduler scheduler = createScheduler(true);
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
    void shouldKeepSwitchValueForWholeBootWhenConfigurationChangesDuringHealthWait() throws Exception {
        ScriptScheduler scheduler = createScheduler(true);
        when(healthChecker.checkStatus()).thenAnswer(invocation -> {
            MockOsgi.modified(shutdownSafety, bundleContext, Collections.singletonMap("enabled", false));
            return new HealthStatus(Collections.emptyList(), true);
        });

        scheduler.process(bootJob());

        InOrder order = inOrder(healthChecker, existingCronSchedule);
        order.verify(healthChecker).checkStatus();
        order.verify(existingCronSchedule).unschedule();
        verify(existingCronSchedule, times(1)).unschedule();
    }

    private ScriptScheduler createScheduler(boolean enabled) throws Exception {
        MockOsgi.activate(shutdownSafety, bundleContext, Collections.singletonMap("enabled", enabled));

        bundleContext.registerService(ResourceResolverFactory.class, repositoryWithAutomaticScriptsFolder(), null);
        bundleContext.registerService(InstanceInfo.class, mock(InstanceInfo.class), null);
        bundleContext.registerService(Executor.class, executor, null);
        bundleContext.registerService(JobManager.class, jobManagerWithOneCronSchedule(), null);
        bundleContext.registerService(HealthChecker.class, healthChecker, null);
        bundleContext.registerService(ExecutionQueue.class, mock(ExecutionQueue.class), null);
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
    private JobManager jobManagerWithOneCronSchedule() {
        when(jobManager.getScheduledJobs(eq(ScriptScheduler.JOB_TOPIC), eq(-1L), any()))
                .thenAnswer(invocation -> {
                    Map<String, Object> filter = invocation.getArgument(2);
                    boolean cron =
                            ScriptScheduler.JobType.CRON.name().equals(filter.get(ScriptScheduler.JOB_PROP_TYPE));
                    Collection<ScheduledJobInfo> found =
                            cron ? Collections.singletonList(existingCronSchedule) : Collections.emptyList();
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
}
