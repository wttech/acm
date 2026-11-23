package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.instance.ShutdownSafety;
import dev.vml.es.acm.core.notification.NotificationManager;
import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceRole;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.repo.Locker;
import dev.vml.es.acm.core.script.ScriptType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.jcr.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.slf4j.helpers.MessageFormatter;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ExecutorTest {

    private static final String SCRIPT_ID = "/conf/acm/settings/script/automatic/test.groovy";

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    private final OsgiContext osgiContext = mock(OsgiContext.class);

    private final ExecutionQueue queue = mock(ExecutionQueue.class);

    private final NotificationManager notifier = mock(NotificationManager.class);

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    private static final String EXECUTION_ID = "2026/11/18/11/33/job_1";

    private static final String EXECUTION_ID_OF_DEAD_RUN = "2026/11/18/10/02/job_7";

    private static final String ANOTHER_RUN_EXECUTION_ID = "2026/11/18/11/40/job_2";

    private static final String LOCAL_INSTANCE = "local-instance";

    private static final String GONE_INSTANCE = "gone-instance";

    private static final String OWNER_INSTANCE = "owner-instance";

    private static final String SCRIPT_LOCK = Executor.LOCK_DIR + "/script/automatic/test.groovy";

    private static final String MANUAL_SCRIPT_ID = "/conf/acm/settings/script/manual/test.groovy";

    private static final String MANUAL_SCRIPT_LOCK = Executor.LOCK_DIR + "/script/manual/test.groovy";

    private static final long ONE_MINUTE = 60_000L;

    private static final long ALIVE_TIMEOUT_MILLIS = 30_000L;

    private static final long ATTEMPT_LIMIT = 2L;

    private String failingDeletePath;

    private boolean deleteFailsWithError;

    @AfterEach
    void clearLogs() {
        TestLoggerFactory.clear();
    }

    /** What happens while the script runs; scripts call it as `duringRun.run()`. */
    private Consumer<ExecutionContext> duringRun = context -> {};

    private final JobManager jobManager = mock(JobManager.class);

    private final AtomicInteger completedRunsSeenByExtension = new AtomicInteger();

    @Test
    void shouldAbortGracefullyWhenUserStopsExecution() {
        queueIsRunning(true);

        Execution execution = run("void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertFalse(abortedForLostLock(execution));
    }

    @Test
    void shouldSucceedWhenQueueIsRunning() {
        queueIsRunning(false);

        Execution execution = run("void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertFalse(abortedForLostLock(execution));
    }

    @Test
    void shouldFailWhenQueueIsGone() {
        queueIsGone();

        Execution execution = run("void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertFalse(abortedForLostLock(execution));
    }

    @Test
    void shouldFailAndReleaseLockWhenShutdownSafetyIsEnabledAndQueueIsGone() throws Exception {
        queueIsGone();
        Map<String, Object> shutdownSafetyConfig = shutdownSafetyEnabled();

        Execution execution = run(createExecutor(shutdownSafetyConfig), "void doRun() { context.checkAborted() }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertFalse(abortedForLostLock(execution));
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldAbortForLostLockWhenLockWasLostBeforeQueueIsGone() {
        queueIsGone();
        duringRun = ExecutionContext::stopForLostLock;

        Execution execution = run(
                createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run(); context.checkAborted() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertTrue(abortedForLostLock(execution));
    }

    @Test
    void shouldFailWhenUnlockFailsAndShutdownSafetyIsDisabled() {
        lockReleaseFails();

        Execution execution = run("void doRun() { }");

        assertEquals(ExecutionStatus.FAILED, execution.getStatus());
        assertTrue(execution.getError().contains("Cannot delete lock"));
    }

    @Test
    void shouldUseLockTimeoutSetByScriptWhenShutdownSafetyIsDisabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();

        run("void describeRun() { context.lockTimeout = '2h' }\nvoid doRun() { }");

        Calendar threeHoursAhead = Calendar.getInstance();
        threeHoursAhead.add(Calendar.HOUR, 3);
        assertTrue(scriptLockNode(SCRIPT_LOCK)
                .get(Locker.LOCKED_UNTIL_PROP, Calendar.class)
                .before(threeHoursAhead));
    }

    @Test
    void shouldThrowAndWarnWhenLockIsLeftBehindAndShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\nvoid doRun() { }");

        AcmException failure = assertThrows(AcmException.class, () -> executor.execute(context));
        assertTrue(failure.getMessage().contains("is left behind"));
        assertNotNull(lockResource(SCRIPT_LOCK));
        assertLeftBehindWarnings();
    }

    @Test
    void shouldThrowAndWarnWhenLockReleaseFailsWithError() throws Exception {
        queueIsRunning(false);
        lockReleaseFailsWithError();
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\nvoid doRun() { }");

        AcmException failure = assertThrows(AcmException.class, () -> executor.execute(context));
        assertTrue(failure.getMessage().contains("is left behind"));
        assertLeftBehindWarnings();
    }

    @Test
    void shouldSaveHistoryBeforeReportingLockLeftBehind() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\nvoid doRun() { }");
        context.setNotifications(false);

        assertThrows(AcmException.class, () -> executor.execute(context));
        assertNotNull(historyEntry(EXECUTION_ID));
    }

    @Test
    void shouldSaveSucceededScriptAsSucceededWhenLockIsLeftBehind() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\nvoid doRun() { }");
        context.setNotifications(false);

        assertThrows(AcmException.class, () -> executor.execute(context));
        assertEquals(
                ExecutionStatus.SUCCEEDED.name(),
                historyEntry(EXECUTION_ID).getValueMap().get("status", String.class));
    }

    @Test
    void shouldSaveScriptErrorNotReleaseErrorWhenLockIsLeftBehind() throws Exception {
        queueIsRunning(false);
        lockReleaseFailsWithError();
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext context = createContext(
                executor,
                "boolean canRun() { true }\nvoid doRun() { throw new IllegalStateException('script broke') }");
        context.setNotifications(false);

        assertThrows(AcmException.class, () -> executor.execute(context));
        ValueMap historyEntry = historyEntry(EXECUTION_ID).getValueMap();
        assertEquals(ExecutionStatus.FAILED.name(), historyEntry.get("status", String.class));
        assertTrue(historyEntry.get("error", String.class).contains("script broke"));
    }

    @Test
    void shouldUseLockTimeoutSetByScriptWhenShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();

        runLeavingLockBehind(
                createExecutor(shutdownSafetyEnabled()),
                "void describeRun() { context.lockTimeout = '2h' }\nvoid doRun() { }");

        Calendar threeHoursAhead = Calendar.getInstance();
        threeHoursAhead.add(Calendar.HOUR, 3);
        assertTrue(scriptLockNode(SCRIPT_LOCK)
                .get(Locker.LOCKED_UNTIL_PROP, Calendar.class)
                .before(threeHoursAhead));
    }

    @Test
    void shouldWriteNoExecutionIdOnLockWhenShutdownSafetyIsDisabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();

        run("void doRun() { }");

        ValueMap lockNode = scriptLockNode(SCRIPT_LOCK);
        assertFalse(lockNode.containsKey("executionId"));
        assertFalse(lockNode.containsKey("attempt"));
        assertFalse(lockNode.containsKey("alive"));
    }

    @Test
    void shouldRunAgainWhenOwnLockWasLeftByDeadRun() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldCreateRunAndReleaseOwnLockWhenShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldStoreExecutionIdOnLockWhenShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();

        runLeavingLockBehind(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(EXECUTION_ID, scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
    }

    @Test
    void shouldRunAgainWhenCleaningOutputLeftoversFails() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        outputChunkExists(ExecutionContext.varPath(EXECUTION_ID) + "/output/chunk9");
        failingDeleteBelow(ExecutionContext.varPath(EXECUTION_ID));

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }

    @Test
    void shouldUseUsualLockForManualScriptWhenShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();

        run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }", MANUAL_SCRIPT_ID);

        ValueMap lockNode = scriptLockNode(MANUAL_SCRIPT_LOCK);
        assertFalse(lockNode.containsKey("executionId"));
        assertFalse(lockNode.containsKey("attempt"));
        assertFalse(lockNode.containsKey("alive"));
    }

    @Test
    void shouldAbortWhenAnotherRunHoldsTheLockWhenScriptEnds() throws Exception {
        queueIsRunning(false);
        duringRun = context -> lockReplacedBy(SCRIPT_LOCK, ANOTHER_RUN_EXECUTION_ID);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertTrue(abortedForLostLock(execution));
        assertEquals(ANOTHER_RUN_EXECUTION_ID, scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
    }

    @Test
    void shouldAbortWhenScriptReturnsAfterAbortCheckForLostLock() {
        queueIsRunning(false);
        duringRun = this::lockLostToAnotherRun;

        Execution execution = run(
                createExecutor(shutdownSafetyEnabled()),
                "void doRun() { duringRun.run(); if (context.aborted) { return } }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertTrue(abortedForLostLock(execution));
    }

    @Test
    void shouldSaveNoHistoryWhenLaterAttemptOfSameExecutionTookOver() throws Exception {
        queueIsRunning(false);
        duringRun = context -> scriptLockTakenOverByLaterAttempt();

        Execution execution =
                runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertNull(historyEntry(EXECUTION_ID));
    }

    @Test
    void shouldSendNoNotificationWhenLaterAttemptOfSameExecutionTookOver() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();
        duringRun = context -> scriptLockTakenOverByLaterAttempt();

        runWithHistoryAndNotifications(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        verify(notifier, never()).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldAbortForLostLockWhenScriptFailsAfterSeeingLockLost() {
        queueIsRunning(false);
        duringRun = this::lockLostToAnotherRun;

        Execution execution = run(
                createExecutor(shutdownSafetyEnabled()),
                "void doRun() { duringRun.run(); if (context.aborted) { throw new IllegalStateException('write failed') } }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertTrue(abortedForLostLock(execution));
    }

    @Test
    void shouldSendNoNotificationWhenLaterAttemptAlreadyFinished() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();
        duringRun = context -> laterAttemptFinished();

        runWithHistoryAndNotifications(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        verify(notifier, never()).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldReportRunWhenScriptCallsInternalLockMethods() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();

        Execution execution = runWithHistoryAndNotifications(
                createExecutor(shutdownSafetyEnabled()), "void doRun() { context.stopForLostLock() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertNotNull(historyEntry(EXECUTION_ID));
        verify(notifier).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldWriteAuditLineWhenLaterAttemptReportsTheResult() throws Exception {
        queueIsRunning(false);
        duringRun = context -> scriptLockTakenOverByLaterAttempt();

        runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        assertEquals(1, notReportedAuditLines().size());
    }

    @Test
    void shouldCompleteRunWithExtensionScriptsWhenRunReportsItsResult() throws Exception {
        queueIsRunning(false);
        extensionScriptCountsCompletedRuns();

        runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() {}");

        assertEquals(1, completedRunsSeenByExtension.get());
    }

    @Test
    void shouldNotCompleteRunWithExtensionScriptsWhenLaterAttemptReportsTheResult() throws Exception {
        queueIsRunning(false);
        extensionScriptCountsCompletedRuns();
        duringRun = context -> scriptLockTakenOverByLaterAttempt();

        runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        assertEquals(0, completedRunsSeenByExtension.get());
    }

    @Test
    void shouldWriteAuditLineWhenHistoryEntryAppearsDuringRun() throws Exception {
        queueIsRunning(false);
        duringRun = context -> plantedHistoryEntry();

        runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        assertEquals(1, notReportedAuditLines().size());
    }

    @Test
    void shouldWriteNoAuditLineWhenRunReportsItself() throws Exception {
        queueIsRunning(false);

        runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertTrue(notReportedAuditLines().isEmpty());
    }

    @Test
    void shouldSendNotificationWhenLockedBecauseHistoryHasEntryOfThisExecution() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();
        historyEntryExists(EXECUTION_ID);

        Execution execution =
                runWithHistoryAndNotifications(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
        verify(notifier).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldEndLockedWithoutSavingHistoryAgainInDebugModeWhenHistoryHasEntryOfThisExecution() throws Exception {
        queueIsRunning(false);
        historyEntryExists(EXECUTION_ID);
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\nvoid doRun() { }", SCRIPT_ID);
        context.setNotifications(false);
        context.setDebug(true);

        Execution execution = assertDoesNotThrow(() -> executor.execute(context));

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
        assertFalse(historyEntry(EXECUTION_ID).getValueMap().containsKey("status"));
    }

    @Test
    void shouldSaveResultOfRunWhenItsRedeliveredAttemptIsRefusedInDebugMode() throws Exception {
        queueIsRunning(false);
        Executor executor = createExecutor(shutdownSafetyEnabled());
        duringRun = context -> redeliveredAttemptIsRefusedInDebugMode(executor);
        ExecutionContext firstAttempt =
                createContext(executor, "boolean canRun() { true }\nvoid doRun() { duringRun.run() }");
        firstAttempt.setNotifications(false);
        firstAttempt.setDebug(true);

        executor.execute(firstAttempt);

        assertEquals(
                ExecutionStatus.SUCCEEDED.name(),
                historyEntry(EXECUTION_ID).getValueMap().get("status", String.class));
    }

    @Test
    void shouldSendNotificationWhenAnotherRunTookTheLock() throws Exception {
        queueIsRunning(false);
        notifierIsConfigured();
        duringRun = context -> lockReplacedBy(SCRIPT_LOCK, ANOTHER_RUN_EXECUTION_ID);

        runWithHistoryAndNotifications(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        verify(notifier).sendMessageTo(any(), any(), any(), any());
    }

    @Test
    void shouldSaveAbortedHistoryWhenAnotherRunTookTheLock() throws Exception {
        queueIsRunning(false);
        duringRun = context -> lockReplacedBy(SCRIPT_LOCK, ANOTHER_RUN_EXECUTION_ID);

        Execution execution =
                runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { duringRun.run() }");

        assertEquals(ExecutionStatus.ABORTED, execution.getStatus());
        assertNotNull(historyEntry(EXECUTION_ID));
    }

    @Test
    void shouldStayLockedWhenOwnLockWasLeftAndShutdownSafetyIsDisabled() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run("void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenOwnLockWasAlreadyReentered() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 2L, ONE_MINUTE);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenConfiguredAttemptLimitIsReached() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 3L, ONE_MINUTE);
        Map<String, Object> shutdownSafetyConfig = shutdownSafetyEnabled();
        shutdownSafetyConfig.put("lockAttemptLimit", 3L);

        Execution execution = run(createExecutor(shutdownSafetyConfig), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldRunThirdAttemptWhenAttemptLimitAllowsIt() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 2L, ONE_MINUTE);
        Map<String, Object> shutdownSafetyConfig = shutdownSafetyEnabled();
        shutdownSafetyConfig.put("lockAttemptLimit", 3L);

        Execution execution = run(createExecutor(shutdownSafetyConfig), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenEarlierRunIsStillAlive() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, 0L);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenLockBelongsToAnotherExecution() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, ANOTHER_RUN_EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenLockIsFreeAndHistoryHasEntryOfThisExecution() throws Exception {
        queueIsRunning(false);
        historyEntryExists(EXECUTION_ID);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldRunWhenLockIsFreeAndHistoryHasEntryAndShutdownSafetyIsDisabled() throws Exception {
        queueIsRunning(false);
        historyEntryExists(EXECUTION_ID);

        Execution execution = run(createExecutor(Collections.emptyMap()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenHistoryHasEntryOfThisExecution() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        historyEntryExists(EXECUTION_ID);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenScriptIsNotAutomatic() throws Exception {
        queueIsRunning(false);
        lockHeldBy(MANUAL_SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }", MANUAL_SCRIPT_ID);

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldDeleteOutputLeftoversWhenRunningAgain() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        String deadRunOutputChunk = ExecutionContext.varPath(EXECUTION_ID) + "/output/chunk9";
        outputChunkExists(deadRunOutputChunk);

        run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertNull(repository.getServiceResourceResolver(Collections.emptyMap()).getResource(deadRunOutputChunk));
    }

    @Test
    void shouldKeepCountedAttemptWhenReleaseFails() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        lockReleaseFails();

        runLeavingLockBehind(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(2L, scriptLockNode(SCRIPT_LOCK).get("attempt", Long.class));
    }

    @Test
    void shouldReportFailureAndKeepOwnLockWhenQueueIsGoneAndReleaseFails() throws Exception {
        queueIsGone();
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        lockReleaseFails();

        runLeavingLockBehind(createExecutor(shutdownSafetyEnabled()), "void doRun() { context.checkAborted() }");

        assertEquals(EXECUTION_ID, scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
        assertEquals(2L, scriptLockNode(SCRIPT_LOCK).get("attempt", Long.class));
    }

    @Test
    void shouldReportLockedToBootCheckWhileOwnLockExists() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, ONE_MINUTE);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldRunUnderLockLeftByInstanceThatLeftClusterWhenItsJobHasFinished() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);

        Execution execution = runWithHistory(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertNotNull(historyEntry(EXECUTION_ID));
        assertNull(lockResource(SCRIPT_LOCK));
    }

    @Test
    void shouldRunAgainWhenOwnerOfOwnLockLeftClusterAlthoughAliveIsFresh() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenLockOfInstanceThatLeftClusterReachedAttemptLimit() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 2L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenOwnerInstanceIsStillInCluster() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, ONE_MINUTE);
        lockOwnedBy(SCRIPT_LOCK, OWNER_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE, OWNER_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);

        Execution execution = run(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
    }

    @Test
    void shouldStayLockedWhenOwnerInstanceLeftClusterAndShutdownSafetyIsDisabled() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, ONE_MINUTE);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);

        Execution execution = run(createExecutor(Collections.emptyMap()), "void doRun() { }");

        assertEquals(ExecutionStatus.LOCKED, execution.getStatus());
        verifyNoInteractions(jobManager);
    }

    @Test
    void shouldReportLockedToBootCheckWhenJobOfInstanceThatLeftClusterCannotBeFound() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        bundleContext.registerService(JobManager.class, jobManager, null);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldReportLockedToBootCheckForLockOfInstanceThatLeftClusterWithoutJobManager() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldReportLockedToBootCheckWhileJobOfInstanceThatLeftClusterAwaitsDelivery() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.ACTIVE);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldReportNotLockedToBootCheckWhenJobOfInstanceThatLeftClusterWasCancelled() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertFalse(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldReportLockedToBootCheckWhenLockOfInstanceThatLeftClusterReachedAttemptLimit() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 2L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldReportLockedToBootCheckForLockOfInstanceThatLeftClusterWhenShutdownSafetyIsDisabled() throws Exception {
        lockHeldBy(SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);
        Executor executor = createExecutor(Collections.emptyMap());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }")));
    }

    @Test
    void shouldReportLockedToBootCheckForLockOfInstanceThatLeftClusterWhenScriptIsManual() throws Exception {
        lockHeldBy(MANUAL_SCRIPT_LOCK, EXECUTION_ID_OF_DEAD_RUN, 1L, 0L);
        lockOwnedBy(MANUAL_SCRIPT_LOCK, GONE_INSTANCE);
        clusterWithInstances(LOCAL_INSTANCE);
        jobOfDeadRunIs(Job.JobState.GIVEN_UP);
        Executor executor = createExecutor(shutdownSafetyEnabled());

        assertTrue(executor.isLocked(createContext(executor, "void doRun() { }", MANUAL_SCRIPT_ID)));
    }

    @Test
    void shouldRunHealthCheckWithoutTouchingScriptLockWhenShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);
        lockHeldBy(SCRIPT_LOCK, ANOTHER_RUN_EXECUTION_ID, 1L, 0L);
        Executor executor = createExecutor(shutdownSafetyEnabled());
        ExecutionContext healthCheck = createContext(
                executor, ExecutionId.HEALTH_CHECK, "boolean canRun() { true }\nvoid doRun() { }", SCRIPT_ID);
        healthCheck.setHistory(false);
        healthCheck.setNotifications(false);

        Execution execution = executor.execute(healthCheck);

        assertEquals(ExecutionStatus.SUCCEEDED, execution.getStatus());
        assertEquals(ANOTHER_RUN_EXECUTION_ID, scriptLockNode(SCRIPT_LOCK).get("executionId", String.class));
    }

    @Test
    void shouldStoreLocalSlingIdOnLockWhenShutdownSafetyIsEnabled() throws Exception {
        queueIsRunning(false);
        lockReleaseFails();
        clusterWithInstances(LOCAL_INSTANCE);

        runLeavingLockBehind(createExecutor(shutdownSafetyEnabled()), "void doRun() { }");

        assertEquals(LOCAL_INSTANCE, scriptLockNode(SCRIPT_LOCK).get("slingId", String.class));
    }

    private Execution run(String doRun) {
        return run(createExecutor(Collections.emptyMap()), doRun);
    }

    private Execution run(Executor executor, String doRun) {
        return run(executor, doRun, SCRIPT_ID);
    }

    /** The repository refuses to delete the lock, so the run reports a failure to Sling and keeps its lock. */
    private void runLeavingLockBehind(Executor executor, String doRun) throws Exception {
        ExecutionContext context = createContext(executor, "boolean canRun() { true }\n" + doRun, SCRIPT_ID);
        context.setHistory(false);
        context.setNotifications(false);
        assertThrows(AcmException.class, () -> executor.execute(context));
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
            lockHeldBy(SCRIPT_LOCK, EXECUTION_ID, 2L, 0L);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A later attempt of the same execution saved its result and released the lock while this run worked. */
    private void laterAttemptFinished() {
        try {
            ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
            resolver.delete(resolver.getResource(Locker.ROOT + "/" + SCRIPT_LOCK));
            resolver.commit();
            historyEntryExists(EXECUTION_ID);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A history entry of this execution written by someone else, while its lock stays as it was. */
    private void plantedHistoryEntry() {
        try {
            historyEntryExists(EXECUTION_ID);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean abortedForLostLock(Execution execution) {
        return StringUtils.contains(execution.getError(), ExecutionContext.LOCK_LOST_MESSAGE);
    }

    /** The two lines that tell operators why the Sling job of a finished run is reported as failed. */
    private void assertLeftBehindWarnings() {
        List<String> releaseWarnings = warnLines(OwnedRunLock.class, "Execution lock of");
        assertEquals(1, releaseWarnings.size());
        assertTrue(releaseWarnings
                .get(0)
                .startsWith(
                        String.format("Execution lock of '%s' cannot be released by '%s': ", SCRIPT_ID, EXECUTION_ID)));
        assertEquals(
                Collections.singletonList(String.format(
                        "Execution lock of '%s' is left behind by '%s', reporting the job as failed",
                        SCRIPT_ID, EXECUTION_ID)),
                warnLines(Executor.class, "Execution lock of"));
    }

    private List<String> warnLines(Class<?> loggerClass, String prefix) {
        return TestLoggerFactory.getTestLogger(loggerClass).getLoggingEvents().stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(event -> MessageFormatter.arrayFormat(
                                event.getMessage(), event.getArguments().toArray())
                        .getMessage())
                .filter(message -> message.startsWith(prefix))
                .collect(Collectors.toList());
    }

    private List<String> notReportedAuditLines() {
        return TestLoggerFactory.getTestLogger(ExecutionAudit.LOGGER_NAME).getLoggingEvents().stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(event -> MessageFormatter.arrayFormat(
                                event.getMessage(), event.getArguments().toArray())
                        .getMessage())
                .filter(message -> message.startsWith("Code run not reported"))
                .collect(Collectors.toList());
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
        return createContext(executor, EXECUTION_ID, content, scriptId);
    }

    private ExecutionContext createContext(Executor executor, String executionId, String content, String scriptId)
            throws Exception {
        ResourceResolver resolver = scriptResolver();
        ResourceUtil.getOrCreateResource(
                resolver, ScriptType.EXTENSION.root(), "nt:unstructured", "nt:unstructured", true);
        ExecutionContext context = executor.createContext(
                executionId,
                "acm-content-service",
                ExecutionMode.RUN,
                executable(content, scriptId),
                new InputValues(),
                resolver,
                new CodeOutputMemory());
        Runnable duringThisRun = () -> duringRun.accept(context);
        context.getCodeContext().getBinding().setVariable("duringRun", duringThisRun);
        Runnable completedRun = completedRunsSeenByExtension::incrementAndGet;
        context.getCodeContext().getBinding().setVariable("completedRun", completedRun);
        return context;
    }

    /** Extension script that counts the runs it completes, through the "completedRun" binding. */
    private void extensionScriptCountsCompletedRuns() throws Exception {
        String scriptPath = ScriptType.EXTENSION.root() + "/count-completed-runs.groovy";
        String content = "import dev.vml.es.acm.core.code.ExecutionContext\n"
                + "import dev.vml.es.acm.core.code.Execution\n"
                + "void prepareRun(ExecutionContext context) {}\n"
                + "void completeRun(Execution execution) { completedRun.run() }\n";
        ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
        ResourceUtil.getOrCreateResource(resolver, scriptPath, "nt:file", "nt:unstructured", false);
        Map<String, Object> fileContent = new HashMap<>();
        fileContent.put("jcr:primaryType", "nt:resource");
        fileContent.put("jcr:data", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        resolver.create(resolver.getResource(scriptPath), "jcr:content", fileContent);
        resolver.commit();
    }

    /** Sling delivers the same job again while its first attempt still runs, so the new attempt is refused. */
    private void redeliveredAttemptIsRefusedInDebugMode(Executor executor) {
        try {
            ExecutionContext redeliveredAttempt =
                    createContext(executor, "boolean canRun() { true }\nvoid doRun() { }");
            redeliveredAttempt.setNotifications(false);
            redeliveredAttempt.setDebug(true);
            assertEquals(
                    ExecutionStatus.LOCKED, executor.execute(redeliveredAttempt).getStatus());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Another run took the lock and the heartbeat noticed it. */
    private void lockLostToAnotherRun(ExecutionContext context) {
        lockReplacedBy(SCRIPT_LOCK, ANOTHER_RUN_EXECUTION_ID);
        context.stopForLostLock();
    }

    private Executor createExecutor(Map<String, Object> shutdownSafetyConfig) {
        ShutdownSafety shutdownSafety = new ShutdownSafety();
        MockOsgi.activate(shutdownSafety, bundleContext, shutdownSafetyConfig);

        bundleContext.registerService(ResourceResolverFactory.class, faultyRepository(), null);
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

    /** Real in-memory repository that can refuse deletes, like a repository refusing a write. */
    @SuppressWarnings("unchecked")
    private ResourceResolverFactory faultyRepository() {
        ResourceResolverFactory factory = mock(ResourceResolverFactory.class);
        try {
            when(factory.getServiceResourceResolver(any(Map.class))).thenAnswer(invocation -> {
                ResourceResolver resolver = repository.getServiceResourceResolver(invocation.getArgument(0));
                return failingDeletePath != null ? resolverFailingDeleteBelow(resolver) : resolver;
            });
        } catch (LoginException e) {
            throw new IllegalStateException(e);
        }
        return factory;
    }

    /** Current cluster view of Sling Discovery with the local instance and the given instances. */
    private void clusterWithInstances(String... slingIds) {
        Set<InstanceDescription> instances = Arrays.stream(slingIds)
                .map(slingId -> {
                    InstanceDescription instance = mock(InstanceDescription.class);
                    when(instance.getSlingId()).thenReturn(slingId);
                    return instance;
                })
                .collect(Collectors.toSet());
        InstanceDescription local = mock(InstanceDescription.class);
        when(local.getSlingId()).thenReturn(LOCAL_INSTANCE);
        TopologyView view = mock(TopologyView.class);
        when(view.isCurrent()).thenReturn(true);
        when(view.getLocalInstance()).thenReturn(local);
        when(view.getInstances()).thenReturn(instances);
        DiscoveryService discovery = mock(DiscoveryService.class);
        when(discovery.getTopology()).thenReturn(view);
        bundleContext.registerService(DiscoveryService.class, discovery, null);
    }

    /** Sling knows the job of the dead run in the given state. */
    private void jobOfDeadRunIs(Job.JobState state) {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(EXECUTION_ID_OF_DEAD_RUN);
        when(job.getJobState()).thenReturn(state);
        when(jobManager.getJobById(EXECUTION_ID_OF_DEAD_RUN)).thenReturn(job);
        bundleContext.registerService(JobManager.class, jobManager, null);
    }

    private void lockOwnedBy(String lockName, String slingId) throws Exception {
        ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
        resolver.getResource(Locker.ROOT + "/" + lockName)
                .adaptTo(ModifiableValueMap.class)
                .put("slingId", slingId);
        resolver.commit();
    }

    private void queueIsGone() {
        when(osgiContext.getService(ExecutionQueue.class)).thenThrow(new IllegalStateException("Service not found"));
    }

    private void queueIsRunning(boolean stopping) {
        when(osgiContext.getService(ExecutionQueue.class)).thenReturn(queue);
        when(queue.isStoppingOrStopped(EXECUTION_ID)).thenReturn(stopping);
    }

    private Executable executable(String content, String scriptId) {
        Executable executable = mock(Executable.class);
        when(executable.getId()).thenReturn(scriptId);
        when(executable.getContent()).thenReturn(content);
        return executable;
    }

    private Map<String, Object> shutdownSafetyEnabled() {
        Map<String, Object> shutdownSafetyConfig = new HashMap<>();
        shutdownSafetyConfig.put("enabled", true);
        shutdownSafetyConfig.put("lockAliveTimeout", ALIVE_TIMEOUT_MILLIS);
        shutdownSafetyConfig.put("lockAttemptLimit", ATTEMPT_LIMIT);
        return shutdownSafetyConfig;
    }

    /** Lock node of the given execution, with its last sign of life the given time ago. */
    private void lockHeldBy(String lockName, String executionId, long attempt, long aliveMillisAgo) throws Exception {
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

    private void lockReleaseFailsWithError() {
        lockReleaseFails();
        deleteFailsWithError = true;
    }

    private void lockReleaseFails() {
        failingDeleteBelow(Locker.ROOT + "/" + Executor.LOCK_DIR);
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
                        if (deleteFailsWithError) {
                            throw new LinkageError("Repository classes unloaded");
                        }
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
            lockHeldBy(lockName, executionId, 1L, 0L);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
