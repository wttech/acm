package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.notification.NotificationManager;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceRole;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.repo.Locker;
import dev.vml.es.acm.core.script.ScriptType;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Session;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
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

    private final NotificationManager notifier = mock(NotificationManager.class);

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    private final AtomicInteger resolverCalls = new AtomicInteger();

    private static final int UNLOCK_CALL = 3; // resolver calls of a run: 1 read lock, 2 lock, 3 unlock

    // resolver calls of a first run under its own lock: 1 read lock, 2 create lock, 3 check ownership, 4 release
    private static final int OWNERSHIP_CHECK_AFTER_CREATE_CALL = 3;

    private static final int RELEASE_AFTER_CREATE_CALL = 4;

    // resolver calls of a run again under its own lock: 1 read lock, 2 read history, 3 enter again,
    // 4 delete output of the killed run, 5 check ownership, 6 release
    private static final int RELEASE_AFTER_REENTRY_CALL = 6;

    // a run again under its own lock that aborts skips the ownership check: release is call 5
    private static final int RELEASE_AFTER_ABORTED_REENTRY_CALL = 5;

    private final Map<Integer, Runnable> beforeResolverCall = new HashMap<>();

    private static final String EXECUTION_ID = "2026/11/18/11/33/job_1";

    private static final String SCRIPT_LOCK = Executor.LOCK_DIR + "/script/automatic/test.groovy";

    private static final String MANUAL_SCRIPT_ID = "/conf/acm/settings/script/manual/test.groovy";

    private static final String MANUAL_SCRIPT_LOCK = Executor.LOCK_DIR + "/script/manual/test.groovy";

    private static final long ONE_MINUTE = 60_000L;

    private static final long ALIVE_TIMEOUT_MILLIS = 30_000L;

    // a confirmation as old as the whole alive timeout is always past the stop limit (2/3 of it)
    private static final String LATE_LOCK_CONFIRMATION = "context.confirmLockHeldAt(System.nanoTime() - "
            + TimeUnit.MILLISECONDS.toNanos(ALIVE_TIMEOUT_MILLIS) + "L)";

    private int failingResolverCall = -1;

    private String failingDeletePath;

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

    @Test
    void shouldWriteNoExecutionIdOnLockWhenToggleIsOff() throws Exception {
        queueIsRunning(false);
        failingResolverCall = UNLOCK_CALL;

        run(false, "void doRun() { }");

        ValueMap lockNode = scriptLockNode(SCRIPT_LOCK);
        assertFalse(lockNode.containsKey("executionId"));
        assertFalse(lockNode.containsKey("attempt"));
        assertFalse(lockNode.containsKey("alive"));
    }

    @Test
    void shouldRunAgainWhenOwnLockWasLeftByKilledRun() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldCreateRunAndReleaseOwnLockWhenToggleIsOn() throws Exception {
        queueIsRunning(false);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldStoreExecutionIdOnLockWhenToggleIsOn() throws Exception {
        queueIsRunning(false);
        failingResolverCall = RELEASE_AFTER_CREATE_CALL;

        run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(EXECUTION_ID, scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
    }

    @Test
    void shouldRunAgainWhenCleaningOutputLeftoversFails() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        outputChunkExists(ExecutionContext.varPath(EXECUTION_ID) + "/output/chunk9");
        failingDeleteBelow(ExecutionContext.varPath(EXECUTION_ID));

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }

    @Test
    void shouldUseTodaysLockForManualScriptWhenToggleIsOn() throws Exception {
        queueIsRunning(false);
        failingResolverCall = UNLOCK_CALL;

        run(createExecutor(rerunKilledExecution()), "void doRun() { }", MANUAL_SCRIPT_ID);

        ValueMap lockNode = scriptLockNode(MANUAL_SCRIPT_LOCK);
        assertFalse(lockNode.containsKey("executionId"));
        assertFalse(lockNode.containsKey("attempt"));
        assertFalse(lockNode.containsKey("alive"));
    }

    @Test
    void shouldAbortWhenAnotherRunHoldsTheLockWhenScriptEnds() throws Exception {
        queueIsRunning(false);
        beforeResolverCall.put(
                OWNERSHIP_CHECK_AFTER_CREATE_CALL, () -> lockReplacedBy(SCRIPT_LOCK, "2026/11/18/11/40/job_2"));

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertEquals(AbortCause.EXECUTION_LOCK_LOST, execution.getAbortCause());
        assertEquals("2026/11/18/11/40/job_2", scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
    }

    @Test
    void shouldAbortWhenScriptReturnsAfterAbortCheckForUnconfirmedLock() {
        queueIsRunning(false);

        // the script plays a heartbeat that was late, asks whether to stop and returns
        Execution execution = run(
                createExecutor(rerunKilledExecution()),
                "void doRun() { " + LATE_LOCK_CONFIRMATION + "; if (context.aborted) { return } }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertEquals(AbortCause.EXECUTION_LOCK_LOST, execution.getAbortCause());
    }

    @Test
    void shouldSucceedWhenLockConfirmationIsLateButLockIsStillOwn() {
        queueIsRunning(false);

        // the script plays a heartbeat that was late, then finishes without an abort check
        Execution execution =
                run(createExecutor(rerunKilledExecution()), "void doRun() { " + LATE_LOCK_CONFIRMATION + " }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }

    @Test
    void shouldSaveNoHistoryWhenLaterAttemptOfSameExecutionTookOver() throws Exception {
        queueIsRunning(false);
        beforeResolverCall.put(OWNERSHIP_CHECK_AFTER_CREATE_CALL, this::scriptLockTakenOverByLaterAttempt);

        Execution execution = runWithHistory(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertNull(historyEntry(EXECUTION_ID));
    }

    @Test
    void shouldSendNoNotificationWhenLaterAttemptOfSameExecutionTookOver() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();
        beforeResolverCall.put(OWNERSHIP_CHECK_AFTER_CREATE_CALL, this::scriptLockTakenOverByLaterAttempt);

        runWithHistoryAndNotifications(createExecutor(rerunKilledExecution()), "void doRun() { }");

        verify(notifier, never()).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldSendNotificationWhenAnotherRunTookTheLock() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();
        beforeResolverCall.put(
                OWNERSHIP_CHECK_AFTER_CREATE_CALL, () -> lockReplacedBy(SCRIPT_LOCK, "2026/11/18/11/40/job_2"));

        runWithHistoryAndNotifications(createExecutor(rerunKilledExecution()), "void doRun() { }");

        verify(notifier).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldSaveAbortedHistoryWhenAnotherRunTookTheLock() throws Exception {
        queueIsRunning(false);
        beforeResolverCall.put(
                OWNERSHIP_CHECK_AFTER_CREATE_CALL, () -> lockReplacedBy(SCRIPT_LOCK, "2026/11/18/11/40/job_2"));

        Execution execution = runWithHistory(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertNotNull(historyEntry(EXECUTION_ID));
    }

    @Test
    void shouldStayLockedWhenOwnLockWasLeftAndToggleIsOff() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run(false, "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenOwnLockWasAlreadyReentered() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 2L, ONE_MINUTE);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenEarlierRunIsStillAlive() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, 0L);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenLockBelongsToAnotherExecution() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, "2026/11/18/11/40/job_2", 1L, ONE_MINUTE);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenHistoryHasEntryOfThisExecution() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        historyEntryExists(EXECUTION_ID);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenScriptIsNotAutomatic() throws Exception {
        queueIsRunning(false);
        lockLeftBy(MANUAL_SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run(createExecutor(rerunKilledExecution()), "void doRun() { }", MANUAL_SCRIPT_ID);

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldDeleteOutputLeftoversWhenRunningAgain() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        String killedRunOutputChunk = ExecutionContext.varPath(EXECUTION_ID) + "/output/chunk9";
        outputChunkExists(killedRunOutputChunk);

        run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertNull(repository.getServiceResourceResolver(Collections.emptyMap()).getResource(killedRunOutputChunk));
    }

    @Test
    void shouldKeepCountedAttemptWhenReleaseFails() throws Exception {
        queueIsRunning(false);
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        failingResolverCall = RELEASE_AFTER_REENTRY_CALL;

        run(createExecutor(rerunKilledExecution()), "void doRun() { }");

        assertEquals(2L, scriptLockNode(SCRIPT_LOCK).get("attempt", Long.class));
    }

    @Test
    void shouldKeepShutdownAbortAndOwnLockWhenReleaseFails() throws Exception {
        queueIsGone();
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        failingResolverCall = RELEASE_AFTER_ABORTED_REENTRY_CALL;
        Map<String, Object> toggles = rerunKilledExecution();
        toggles.put("abortOnShutdown", true);

        Execution execution = run(createExecutor(toggles), "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertEquals(EXECUTION_ID, scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
    }

    @Test
    void shouldReportLockedToBootCheckWhileOwnLockExists() throws Exception {
        lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        Executor executor = createExecutor(rerunKilledExecution());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    private Execution run(boolean abortOnShutdown, String doRun) {
        return run(createExecutor(abortOnShutdown), doRun);
    }

    private Execution run(Executor executor, String doRun) {
        return run(executor, doRun, SCRIPT_ID);
    }

    private Execution runWithHistory(Executor executor, String doRun) throws Exception {
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\n" + doRun, SCRIPT_ID);
        context.setNotifications(false);
        return executor.execute(context);
    }

    private Execution runWithHistoryAndNotifications(Executor executor, String doRun) throws Exception {
        return executor.execute(createContext(executor, "boolean canRun() { true }\n" + doRun, SCRIPT_ID));
    }

    private void notifierIsConfigured() {
        when(notifier.isConfigured(any())).thenReturn(true);
        InstanceInfo authorInstance = mock(InstanceInfo.class);
        when(authorInstance.getRole()).thenReturn(InstanceRole.AUTHOR);
        when(osgiContext.getInstanceInfo()).thenReturn(authorInstance);
    }

    private Resource historyEntry(String executionId) throws LoginException {
        return repository
                .getServiceResourceResolver(Collections.emptyMap())
                .getResource(ExecutionHistory.ROOT + "/" + executionId);
    }

    /** A later attempt of the same execution took the lock over while this run worked. */
    private void scriptLockTakenOverByLaterAttempt() {
        try {
            ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
            resolver.delete(resolver.getResource(Locker.ROOT + "/" + SCRIPT_LOCK));
            resolver.commit();
            lockLeftBy(SCRIPT_LOCK, EXECUTION_ID, 2L, 0L);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Execution run(Executor executor, String doRun, String scriptId) {
        try {
            ExecutionContext context = createContext(executor, "boolean canRun() { true }\n" + doRun, scriptId);
            context.setHistory(false);
            context.setNotifications(false);
            return executor.execute(context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ExecutionContext createContext(Executor executor, String content) throws Exception {
        return createContext(executor, content, SCRIPT_ID);
    }

    private ExecutionContext createContext(Executor executor, String content, String scriptId) throws Exception {
        ResourceResolver resolver = scriptResolver();
        ResourceUtil.getOrCreateResource(
                resolver, ScriptType.EXTENSION.root(), "nt:unstructured", "nt:unstructured", true);
        return executor.createContext(
                EXECUTION_ID,
                "acm-content-service",
                ExecutionMode.RUN,
                executable(content, scriptId),
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
        bundleContext.registerService(NotificationManager.class, notifier, null);
        bundleContext.registerService(SpaSettings.class, mock(SpaSettings.class), null);
        bundleContext.registerService(ShutdownSafety.class, shutdownSafety, null);

        Executor executor = new Executor();
        MockOsgi.injectServices(executor, bundleContext);
        MockOsgi.activate(executor, bundleContext, Collections.emptyMap());
        return executor;
    }

    /** In-memory repository with a fake JCR session. The scripts here use no users, ACLs or replication. */
    private ResourceResolver scriptResolver() throws LoginException {
        ResourceResolver resolver = spy(repository.getServiceResourceResolver(Collections.emptyMap()));
        doReturn(mock(JackrabbitSession.class, RETURNS_DEEP_STUBS))
                .when(resolver)
                .adaptTo(Session.class);
        return resolver;
    }

    /** Real in-memory repository. The call number in failingResolverCall fails, like on a stopping instance. */
    @SuppressWarnings("unchecked")
    private ResourceResolverFactory countingRepository() {
        ResourceResolverFactory factory = mock(ResourceResolverFactory.class);
        try {
            when(factory.getServiceResourceResolver(any(Map.class))).thenAnswer(invocation -> {
                int call = resolverCalls.incrementAndGet();
                beforeResolverCall.getOrDefault(call, () -> {}).run();
                if (call == failingResolverCall) {
                    throw new LoginException("Repository unavailable");
                }
                ResourceResolver resolver = repository.getServiceResourceResolver(invocation.getArgument(0));
                return failingDeletePath != null ? resolverFailingDeleteBelow(resolver) : resolver;
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
        when(queue.isStoppingOrStopped(EXECUTION_ID)).thenReturn(stopping);
    }

    private Executable executable(String content, String scriptId) {
        Executable executable = mock(Executable.class);
        when(executable.getId()).thenReturn(scriptId);
        when(executable.getContent()).thenReturn(content);
        return executable;
    }

    private Map<String, Object> rerunKilledExecution() {
        Map<String, Object> toggles = new HashMap<>();
        toggles.put("rerunKilledExecution", true);
        toggles.put("rerunKilledExecutionAliveTimeout", ALIVE_TIMEOUT_MILLIS);
        return toggles;
    }

    /** Lock node left by a killed run of the given execution, with its last sign of life some time ago. */
    private void lockLeftBy(String lockName, String executionId, long attempt, long aliveMillisAgo) throws Exception {
        Calendar lockedAt = Calendar.getInstance();
        lockedAt.add(Calendar.MINUTE, -5);
        Calendar lockedUntil = Calendar.getInstance();
        lockedUntil.add(Calendar.HOUR, 1);
        Calendar alive = Calendar.getInstance();
        alive.setTimeInMillis(alive.getTimeInMillis() - aliveMillisAgo);
        Map<String, Object> props = new HashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("locked", lockedAt);
        props.put("lockedUntil", lockedUntil);
        props.put("executionId", executionId);
        props.put("attempt", attempt);
        props.put("alive", alive);
        props.put("aliveTimeout", ALIVE_TIMEOUT_MILLIS);
        ResourceUtil.getOrCreateResource(
                repository.getServiceResourceResolver(Collections.emptyMap()),
                Locker.ROOT + "/" + lockName,
                props,
                "sling:Folder",
                true);
    }

    private void historyEntryExists(String executionId) throws Exception {
        ResourceUtil.getOrCreateResource(
                repository.getServiceResourceResolver(Collections.emptyMap()),
                ExecutionHistory.ROOT + "/" + executionId,
                "nt:unstructured",
                "sling:Folder",
                true);
    }

    private void outputChunkExists(String chunkPath) throws Exception {
        ResourceUtil.getOrCreateResource(
                repository.getServiceResourceResolver(Collections.emptyMap()),
                chunkPath,
                "nt:unstructured",
                "sling:Folder",
                true);
    }

    private Resource lockResource(String lockName) throws LoginException {
        return repository.getServiceResourceResolver(Collections.emptyMap()).getResource(Locker.ROOT + "/" + lockName);
    }

    private ValueMap scriptLockNode(String lockName) throws LoginException {
        return lockResource(lockName).getValueMap();
    }

    private void failingDeleteBelow(String path) {
        failingDeletePath = path;
    }

    /** Resolver that cannot delete under failingDeletePath, like a repository that refuses a write. */
    private ResourceResolver resolverFailingDeleteBelow(ResourceResolver resolver) throws Exception {
        ResourceResolver failing = spy(resolver);
        doAnswer(invocation -> {
                    Resource resource = invocation.getArgument(0);
                    if (resource.getPath().startsWith(failingDeletePath)) {
                        throw new PersistenceException("Delete refused");
                    }
                    return invocation.callRealMethod();
                })
                .when(failing)
                .delete(any(Resource.class));
        return failing;
    }

    private void lockReplacedBy(String lockName, String executionId) {
        try {
            ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
            resolver.delete(resolver.getResource(Locker.ROOT + "/" + lockName));
            resolver.commit();
            lockLeftBy(lockName, executionId, 1L, 0L);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
