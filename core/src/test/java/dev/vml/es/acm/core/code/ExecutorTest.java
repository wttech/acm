package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.notification.NotificationManager;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.script.ScriptType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Session;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

class ExecutorTest {

    private static final String SCRIPT_ID = "/conf/acm/settings/script/automatic/test.groovy";

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    private final OsgiContext osgiContext = mock(OsgiContext.class);

    private final ExecutionQueue queue = mock(ExecutionQueue.class);

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    private final AtomicInteger resolverCalls = new AtomicInteger();

    private static final int UNLOCK_CALL = 3; // resolver calls of a run: 1 read lock, 2 lock, 3 unlock

    private int failingResolverCall = -1;

    @Test
    void shouldAbortWithShutdownCauseWhenQueueIsGone() {
        queueIsGone();

        Execution execution = run(true, "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, execution.getAbortCause());
    }

    @Test
    void shouldAbortWithShutdownCauseWhenScriptReturnsAfterAbortCheck() {
        queueIsGone();

        Execution execution = run(true, "void doRun() { if (context.aborted) { return } }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, execution.getAbortCause());
    }

    @Test
    void shouldAbortWithoutCauseWhenUserStopsExecution() {
        queueIsRunning(true);

        Execution execution = run(true, "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertNull(execution.getAbortCause());
    }

    @Test
    void shouldSucceedWhenQueueIsRunning() {
        queueIsRunning(false);

        Execution execution = run(true, "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertNull(execution.getAbortCause());
    }

    @Test
    void shouldAbortWithShutdownCauseWhenScriptFailsAfterSeeingShutdown() {
        queueIsGone();

        Execution execution =
                run(true, "void doRun() { if (context.aborted) { throw new IllegalStateException('repo gone') } }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, execution.getAbortCause());
    }

    @Test
    void shouldFailWithoutCauseWhenToggleIsOffAndQueueIsGone() {
        queueIsGone();

        Execution execution = run(false, "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertNull(execution.getAbortCause());
    }

    @Test
    void shouldFailWithoutCauseWhenOnlyOtherTogglesAreOnAndQueueIsGone() {
        queueIsGone();
        Map<String, Object> otherToggles = new HashMap<>();
        otherToggles.put("keepCronUntilHealthy", true);
        otherToggles.put("bootOnInstanceLeft", true);

        Execution execution = run(createExecutor(otherToggles), "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertNull(execution.getAbortCause());
    }

    @Test
    void shouldKeepShutdownAbortAndLockWhenUnlockFails() {
        queueIsGone();
        failingResolverCall = UNLOCK_CALL;
        Executor executor = createExecutor(true);

        Execution abortedRun = run(executor, "void doRun() { context.checkAborted() }");
        Execution nextRun = run(executor, "void doRun() { }");

        assertEquals(ExecutionStatus.ABORTED, abortedRun.getStatus());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, abortedRun.getAbortCause());
        assertEquals(ExecutionStatus.LOCKED, nextRun.getStatus());
    }

    @Test
    void shouldFailWhenUnlockFailsWithoutShutdown() {
        queueIsRunning(false);
        failingResolverCall = UNLOCK_CALL;

        Execution execution = run(true, "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertTrue(execution.getError().contains("Repository unavailable"));
    }

    @Test
    void shouldFailWhenUnlockFailsAndToggleIsOff() {
        failingResolverCall = UNLOCK_CALL;

        Execution execution = run(false, "void doRun() { }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertTrue(execution.getError().contains("Repository unavailable"));
    }

    @Test
    void shouldTakeToggleSnapshotWhenCreatingContext() throws Exception {
        Executor executor = createExecutor(true);

        ExecutionContext context = createContext(executor, "void doRun() { }");

        assertTrue(context.isAbortOnShutdown());
    }

    private Execution run(boolean abortOnShutdown, String doRun) {
        return run(createExecutor(abortOnShutdown), doRun);
    }

    private Execution run(Executor executor, String doRun) {
        try {
            ExecutionContext context = createContext(executor, "boolean canRun() { true }\n" + doRun);
            context.setHistory(false);
            context.setNotifications(false);
            return executor.execute(context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ExecutionContext createContext(Executor executor, String content) throws Exception {
        ResourceResolver resolver = scriptResolver();
        ResourceUtil.getOrCreateResource(
                resolver, ScriptType.EXTENSION.root(), "nt:unstructured", "nt:unstructured", true);
        return executor.createContext(
                "exec-1",
                "acm-content-service",
                ExecutionMode.RUN,
                executable(content),
                new InputValues(),
                resolver,
                new CodeOutputMemory());
    }

    private Executor createExecutor(boolean abortOnShutdown) {
        return createExecutor(Collections.singletonMap("abortOnShutdown", abortOnShutdown));
    }

    private Executor createExecutor(Map<String, Object> toggles) {
        ShutdownSafety shutdownSafety = new ShutdownSafety();
        MockOsgi.activate(shutdownSafety, bundleContext, toggles);

        bundleContext.registerService(ResourceResolverFactory.class, countingRepository(), null);
        bundleContext.registerService(OsgiContext.class, osgiContext, null);
        bundleContext.registerService(NotificationManager.class, mock(NotificationManager.class), null);
        bundleContext.registerService(SpaSettings.class, mock(SpaSettings.class), null);
        bundleContext.registerService(ShutdownSafety.class, shutdownSafety, null);

        Executor executor = new Executor();
        MockOsgi.injectServices(executor, bundleContext);
        MockOsgi.activate(executor, bundleContext, Collections.emptyMap());
        return executor;
    }

    /** In-memory repository with a fake JCR session; the scripts here use no users, ACLs or replication. */
    private ResourceResolver scriptResolver() throws LoginException {
        ResourceResolver resolver = spy(repository.getServiceResourceResolver(Collections.emptyMap()));
        doReturn(mock(JackrabbitSession.class, RETURNS_DEEP_STUBS))
                .when(resolver)
                .adaptTo(Session.class);
        return resolver;
    }

    /** Real in-memory repository; the call number set in failingResolverCall fails like a stopping instance. */
    @SuppressWarnings("unchecked")
    private ResourceResolverFactory countingRepository() {
        ResourceResolverFactory factory = mock(ResourceResolverFactory.class);
        try {
            when(factory.getServiceResourceResolver(any(Map.class))).thenAnswer(invocation -> {
                if (resolverCalls.incrementAndGet() == failingResolverCall) {
                    throw new LoginException("Repository unavailable");
                }
                return repository.getServiceResourceResolver(invocation.getArgument(0));
            });
        } catch (LoginException e) {
            throw new IllegalStateException(e);
        }
        return factory;
    }

    private void queueIsGone() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.empty());
        when(osgiContext.getService(ExecutionQueue.class)).thenThrow(new IllegalStateException("Service not found"));
    }

    private void queueIsRunning(boolean stopping) {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        when(osgiContext.getService(ExecutionQueue.class)).thenReturn(queue);
        when(queue.isStoppingOrStopped("exec-1")).thenReturn(stopping);
    }

    private Executable executable(String content) {
        Executable executable = mock(Executable.class);
        when(executable.getId()).thenReturn(SCRIPT_ID);
        when(executable.getContent()).thenReturn(content);
        return executable;
    }
}
