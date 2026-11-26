package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.Locker;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

class ExecutionLockTest {

    private static final String LOCK_NAME = "executor/script/automatic/test.groovy";

    private static final String OWN_EXECUTION = "2026/11/18/11/33/job_1";

    private static final String OTHER_EXECUTION = "2026/11/18/11/40/job_2";

    private static final Duration LOCK_TIMEOUT = Duration.ofHours(1);

    private static final long ONE_MINUTE = 60_000L;

    private static final long ALIVE_TIMEOUT = 30_000L;

    private static final long ATTEMPT_LIMIT = 2L;

    private static final String LOCAL_INSTANCE = "local-instance";

    private static final String GONE_INSTANCE = "gone-instance";

    private static final String OWNER_INSTANCE = "owner-instance";

    private static final String FASTER_EXECUTION = "2026/11/18/11/41/job_3";

    private final ResourceResolver resolver = newResolver();

    private final LockOwnerPresence presence = mock(LockOwnerPresence.class);

    private final LockOwnerJob ownerJob = mock(LockOwnerJob.class);

    private final ExecutionLock ownLock =
            new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);

    @Test
    void shouldCreateLockWithExecutionIdFirstAttemptAndAliveTime() {
        assertTrue(ownLock.create(resolver, LOCK_TIMEOUT));

        ValueMap lockNode = lockNode();
        assertEquals(OWN_EXECUTION, lockNode.get("executionId", String.class));
        assertEquals(1L, lockNode.get("attempt", Long.class));
        assertNotNull(lockNode.get("alive", Calendar.class));
    }

    @Test
    void shouldDeriveRefreshIntervalFromAliveTimeout() {
        assertEquals(5_000L, ownLock.getAliveIntervalMillis());
    }

    @Test
    void shouldUseMinimumForTooShortTimeoutStoredOnLock() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, 1_000L);
        lockNodeChange().put("aliveTimeout", 0L);
        resolver.commit();

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldStoreAliveTimeoutOnLock() {
        ownLock.create(resolver, LOCK_TIMEOUT);

        assertEquals(ALIVE_TIMEOUT, lockNode().get("aliveTimeout", Long.class));
    }

    @Test
    void shouldJudgeAliveTimeByTimeoutStoredOnLock() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, 40_000L);
        lockNodeChange().put("aliveTimeout", 60_000L);
        resolver.commit();

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldTreatOwnLockWithoutAliveTimeoutAsStillAlive() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);
        lockNodeChange().remove("aliveTimeout");
        resolver.commit();

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldNoteTakeoverByLaterAttemptOfSameExecution() throws Exception {
        ExecutionLock pausedFirstAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        pausedFirstAttempt.create(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);
        ExecutionLock secondAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        secondAttempt.reenter(resolver, LOCK_TIMEOUT);

        assertFalse(pausedFirstAttempt.signalAlive(resolver));
        secondAttempt.release(resolver);

        assertTrue(pausedFirstAttempt.isTakenOverByLaterAttempt());
    }

    @Test
    void shouldNoteTakeoverByLaterAttemptWhenReleasing() throws Exception {
        ExecutionLock pausedFirstAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        pausedFirstAttempt.create(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);
        new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob)
                .reenter(resolver, LOCK_TIMEOUT);

        assertFalse(pausedFirstAttempt.release(resolver));

        assertTrue(pausedFirstAttempt.isTakenOverByLaterAttempt());
    }

    @Test
    void shouldOnlyReadLockWhenCheckingWhetherItIsHeld() throws Exception {
        ExecutionLock pausedFirstAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        pausedFirstAttempt.create(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);
        new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob)
                .reenter(resolver, LOCK_TIMEOUT);

        assertFalse(pausedFirstAttempt.isHeld(resolver));

        assertFalse(pausedFirstAttempt.isTakenOverByLaterAttempt());
    }

    @Test
    void shouldNotNoteTakeoverWhenAnotherExecutionHoldsLock() throws Exception {
        ownLock.create(resolver, LOCK_TIMEOUT);
        lockNodeChange().put("executionId", OTHER_EXECUTION);
        lockNodeChange().put("attempt", 2L);
        resolver.commit();

        assertFalse(ownLock.signalAlive(resolver));
        assertFalse(ownLock.release(resolver));
        assertFalse(ownLock.isTakenOverByLaterAttempt());
    }

    @Test
    void shouldRememberAttemptOnReentry() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);

        ownLock.reenter(resolver, LOCK_TIMEOUT);

        assertEquals(2L, ownLock.getAttempt());
    }

    @Test
    void shouldCreateLockWhenFirstCommitFails() throws Exception {
        lockFolderExists();
        ResourceResolver resolverFailingOnce = resolverFailingCommits(1);

        assertTrue(ownLock.create(resolverFailingOnce, LOCK_TIMEOUT));
        assertEquals(OWN_EXECUTION, lockNode().get("executionId", String.class));
    }

    @Test
    void shouldFailWhenLockCannotBeCreated() throws Exception {
        lockFolderExists();
        ResourceResolver resolverRefusingWrites = resolverFailingCommits(Integer.MAX_VALUE);

        assertThrows(AcmException.class, () -> ownLock.create(resolverRefusingWrites, LOCK_TIMEOUT));
    }

    @Test
    void shouldRefuseReentryWhenCommitFails() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);

        assertFalse(ownLock.reenter(resolverFailingCommits(1), LOCK_TIMEOUT));
        // the mock repository keeps reverted changes, so the test checks the state of the lock object
        assertEquals(0L, ownLock.getAttempt());
    }

    @Test
    void shouldFailAliveSignalWhenCommitFails() throws Exception {
        ownLock.create(resolver, LOCK_TIMEOUT);
        ResourceResolver resolverRefusingWrites = resolverFailingCommits(1);

        assertThrows(AcmException.class, () -> ownLock.signalAlive(resolverRefusingWrites));
    }

    @Test
    void shouldReleaseOwnLockWhenFirstCommitFails() throws Exception {
        ownLock.create(resolver, LOCK_TIMEOUT);

        assertTrue(ownLock.release(resolverFailingCommits(1)));
        assertNull(resolver.getResource(Locker.ROOT + "/" + LOCK_NAME));
    }

    @Test
    void shouldFailWhenOwnLockCannotBeReleased() throws Exception {
        ownLock.create(resolver, LOCK_TIMEOUT);
        ResourceResolver resolverRefusingWrites = resolverFailingCommits(Integer.MAX_VALUE);

        assertThrows(AcmException.class, () -> ownLock.release(resolverRefusingWrites));
    }

    @Test
    void shouldTreatNodeWithoutLockTimeAsFree() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockNodeChange().remove("locked");
        resolver.commit();

        assertEquals(ExecutionLock.State.FREE, ownLock.read(resolver));
        assertTrue(ownLock.create(resolver, LOCK_TIMEOUT));
    }

    @Test
    void shouldCreateLockThatLockerReadsAsActive() {
        ownLock.create(resolver, LOCK_TIMEOUT);

        assertTrue(new Locker(resolver).isLocked(LOCK_NAME));
    }

    @Test
    void shouldRefuseLockOfAnotherExecution() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);

        assertEquals(ExecutionLock.State.OTHER_RUN, ownLock.read(resolver));
        assertFalse(ownLock.create(resolver, LOCK_TIMEOUT));
        assertEquals(OTHER_EXECUTION, lockNode().get("executionId", String.class));
    }

    @Test
    void shouldRefuseLockWithoutExecutionId() {
        new Locker(resolver).lock(LOCK_NAME, LOCK_TIMEOUT);

        assertEquals(ExecutionLock.State.OTHER_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldReenterOwnLockLeftByDeadRun() throws Exception {
        Calendar deadRunLockedUntil = lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);

        Calendar deadRunLockedAt = lockNode().get("locked", Calendar.class);

        assertEquals(ExecutionLock.State.OWN_LEFT_BY_DEAD_RUN, ownLock.read(resolver));
        assertTrue(ownLock.reenter(resolver, LOCK_TIMEOUT));

        ValueMap lockNode = lockNode();
        assertEquals(2L, lockNode.get("attempt", Long.class));
        assertTrue(lockNode.get("lockedUntil", Calendar.class).after(deadRunLockedUntil));
        assertEquals(deadRunLockedAt, lockNode.get("locked", Calendar.class));
        assertEquals(ExecutionLock.State.OWN_ATTEMPT_LIMIT_REACHED, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseReentryWhenLockBelongsToAnotherExecutionAtWriteTime() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);

        assertFalse(ownLock.reenter(resolver, LOCK_TIMEOUT));
        assertEquals(1L, lockNode().get("attempt", Long.class));
    }

    @Test
    void shouldRefuseReentryWhenLockIsGoneAtWriteTime() {
        assertFalse(ownLock.reenter(resolver, LOCK_TIMEOUT));
        assertNull(resolver.getResource(Locker.ROOT + "/" + LOCK_NAME));
    }

    @Test
    void shouldRefuseSecondReentryOfSameDeadAttempt() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);
        ExecutionLock firstDelivery =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        ExecutionLock secondDelivery =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);

        assertTrue(firstDelivery.reenter(resolver, LOCK_TIMEOUT));
        assertFalse(secondDelivery.reenter(resolver, LOCK_TIMEOUT));
        assertEquals(2L, lockNode().get("attempt", Long.class));
    }

    @Test
    void shouldNotReleaseLockOfLaterAttemptOfSameExecution() throws Exception {
        ExecutionLock pausedFirstAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        pausedFirstAttempt.create(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);
        ExecutionLock secondAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        secondAttempt.reenter(resolver, LOCK_TIMEOUT);

        assertFalse(pausedFirstAttempt.release(resolver));
        assertEquals(2L, lockNode().get("attempt", Long.class));
    }

    @Test
    void shouldNotRefreshAliveTimeOfLaterAttemptOfSameExecution() throws Exception {
        ExecutionLock pausedFirstAttempt =
                new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        pausedFirstAttempt.create(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);
        new ExecutionLock(LOCK_NAME, OWN_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob)
                .reenter(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);

        assertFalse(pausedFirstAttempt.signalAlive(resolver));
        assertEquals(ExecutionLock.State.OWN_ATTEMPT_LIMIT_REACHED, ownLock.read(resolver));
        assertTrue(
                lockNode().get("alive", Calendar.class).getTimeInMillis() < System.currentTimeMillis() - ALIVE_TIMEOUT);
    }

    @Test
    void shouldRefuseOwnLockAtAttemptLimitAlthoughOwnerLeftCluster() throws Exception {
        lockWrittenBy(OWN_EXECUTION, ATTEMPT_LIMIT, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.OWN_ATTEMPT_LIMIT_REACHED, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseOwnLockWhenAttemptLimitIsReached() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 2L, ONE_MINUTE);

        assertEquals(ExecutionLock.State.OWN_ATTEMPT_LIMIT_REACHED, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseOwnLockWithoutAttempt() throws Exception {
        lockWrittenBy(OWN_EXECUTION, null, ONE_MINUTE);

        assertEquals(ExecutionLock.State.OWN_ATTEMPT_LIMIT_REACHED, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseOwnLockWithAttemptBelowOne() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 0L, ONE_MINUTE);

        assertEquals(ExecutionLock.State.OWN_ATTEMPT_LIMIT_REACHED, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseOwnLockWhenEarlierRunIsStillAlive() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, 0L);

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldReplaceExpiredOwnLockStartingAtFirstAttempt() throws Exception {
        expiredLockWrittenBy(OWN_EXECUTION);

        assertEquals(ExecutionLock.State.FREE, ownLock.read(resolver));
        assertTrue(ownLock.create(resolver, LOCK_TIMEOUT));
        assertEquals(1L, lockNode().get("attempt", Long.class));
    }

    @Test
    void shouldReleaseOwnLock() {
        ownLock.create(resolver, LOCK_TIMEOUT);

        assertTrue(ownLock.release(resolver));
        assertNull(resolver.getResource(Locker.ROOT + "/" + LOCK_NAME));
    }

    @Test
    void shouldKeepLockOfAnotherExecutionOnRelease() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);

        assertFalse(ownLock.release(resolver));
        assertEquals(OTHER_EXECUTION, lockNode().get("executionId", String.class));
    }

    @Test
    void shouldRefreshAliveTimeOfOwnLock() throws Exception {
        ownLock.create(resolver, LOCK_TIMEOUT);
        aliveTimeMovedBack(ONE_MINUTE);

        assertTrue(ownLock.signalAlive(resolver));

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldNotRefreshAliveTimeOfAnotherExecution() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);

        assertFalse(ownLock.signalAlive(resolver));
        assertTrue(
                lockNode().get("alive", Calendar.class).getTimeInMillis() < System.currentTimeMillis() - ALIVE_TIMEOUT);
    }

    @Test
    void shouldNotRecreateRemovedLockWhenSignallingAlive() {
        assertFalse(ownLock.signalAlive(resolver));

        assertNull(resolver.getResource(Locker.ROOT + "/" + LOCK_NAME));
    }

    @Test
    void shouldTreatOwnLockWithoutAliveTimeAsStillAlive() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);
        lockNodeChange().remove("alive");
        resolver.commit();

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldStoreSlingIdOnLockOnCreate() {
        when(presence.localSlingId()).thenReturn(LOCAL_INSTANCE);

        ownLock.create(resolver, LOCK_TIMEOUT);

        assertEquals(LOCAL_INSTANCE, lockNode().get("slingId", String.class));
    }

    @Test
    void shouldStoreNoSlingIdWhenLocalInstanceIsUnknown() {
        ownLock.create(resolver, LOCK_TIMEOUT);

        assertFalse(lockNode().containsKey("slingId"));
    }

    @Test
    void shouldStoreSlingIdOfNewOwnerOnReentry() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.localSlingId()).thenReturn(LOCAL_INSTANCE);

        ownLock.reenter(resolver, LOCK_TIMEOUT);

        assertEquals(LOCAL_INSTANCE, lockNode().get("slingId", String.class));
    }

    @Test
    void shouldReadLockOfAnotherExecutionAsLeftByGoneInstanceWhenOwnerLeftCluster() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.LEFT_BY_GONE_INSTANCE, ownLock.read(resolver));
    }

    @Test
    void shouldReadLockOfAnotherExecutionAsOtherRunWhenOwnerIsInCluster() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);
        lockOwnedBy(OWNER_INSTANCE);

        assertEquals(ExecutionLock.State.OTHER_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldReadLockOfGoneInstanceAsOtherRunWhenAttemptLimitIsReached() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, ATTEMPT_LIMIT, ONE_MINUTE);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.OTHER_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldReadLockOfGoneInstanceAsOtherRunWhenAttemptIsBelowOne() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, -100L, ONE_MINUTE);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.OTHER_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldReadLockOfGoneInstanceAsOtherRunWhileItsJobAwaitsDelivery() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);
        when(ownerJob.isAwaitingDelivery(eq(OTHER_EXECUTION))).thenReturn(true);

        assertEquals(ExecutionLock.State.OTHER_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseTakeoverWhenJobOfGoneInstanceAwaitsDeliveryAtWriteTime() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);
        assertEquals(ExecutionLock.State.LEFT_BY_GONE_INSTANCE, ownLock.read(resolver));
        when(ownerJob.isAwaitingDelivery(eq(OTHER_EXECUTION))).thenReturn(true);

        assertFalse(ownLock.takeOver(resolver, LOCK_TIMEOUT));
        assertEquals(OTHER_EXECUTION, lockNode().get("executionId", String.class));
    }

    @Test
    void shouldNotAskSlingForJobWhenOwnerIsInCluster() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, ONE_MINUTE);
        lockOwnedBy(OWNER_INSTANCE);

        ownLock.read(resolver);

        verifyNoInteractions(ownerJob);
    }

    @Test
    void shouldNotAskSlingForJobWhenAttemptLimitIsReached() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, ATTEMPT_LIMIT, ONE_MINUTE);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        ownLock.read(resolver);

        verifyNoInteractions(ownerJob);
    }

    @Test
    void shouldRunAgainUnderOwnLockOfGoneInstanceWhileItsJobIsDelivered() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);
        when(ownerJob.isAwaitingDelivery(eq(OWN_EXECUTION))).thenReturn(true);

        assertEquals(ExecutionLock.State.OWN_LEFT_BY_DEAD_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseReentryWhenLockDisappearsBeforeItIsWritten() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);

        assertFalse(ownLock.reenter(resolverLosingLockAfterCheck(), LOCK_TIMEOUT));
    }

    @Test
    void shouldRefuseTakeoverWhenLockDisappearsBeforeItIsWritten() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertFalse(ownLock.takeOver(resolverLosingLockAfterCheck(), LOCK_TIMEOUT));
    }

    @Test
    void shouldTakeOverLockOfGoneInstanceWithNextAttemptAndOwnIds() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);
        when(presence.localSlingId()).thenReturn(LOCAL_INSTANCE);

        assertTrue(ownLock.takeOver(resolver, LOCK_TIMEOUT));

        ValueMap lockNode = lockNode();
        assertEquals(OWN_EXECUTION, lockNode.get("executionId", String.class));
        assertEquals(2L, lockNode.get("attempt", Long.class));
        assertEquals(LOCAL_INSTANCE, lockNode.get("slingId", String.class));
        assertTrue(ownLock.isHeld(resolver));
    }

    @Test
    void shouldRefuseTakeoverWhenOwnerIsBackInClusterAtWriteTime() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockOwnedBy(OWNER_INSTANCE);

        assertFalse(ownLock.takeOver(resolver, LOCK_TIMEOUT));

        assertEquals(OTHER_EXECUTION, lockNode().get("executionId", String.class));
    }

    @Test
    void shouldReadOwnLockAsLeftByDeadRunWhenOwnerLeftClusterAlthoughAliveIsFresh() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.OWN_LEFT_BY_DEAD_RUN, ownLock.read(resolver));
    }

    @Test
    void shouldReadOwnLockAsStillAliveWhileOwnerInstanceIsInClusterAlthoughAliveIsOld() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, ONE_MINUTE);
        lockOwnedBy(OWNER_INSTANCE);
        when(presence.isOtherInstanceInCluster(OWNER_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldReadOwnLockAsStillAliveWhenOwnerIsInClusterAndAliveIsFresh() throws Exception {
        lockWrittenBy(OWN_EXECUTION, 1L, 0L);
        lockOwnedBy(OWNER_INSTANCE);
        when(presence.isOtherInstanceInCluster(OWNER_INSTANCE)).thenReturn(true);

        assertEquals(ExecutionLock.State.OWN_RUN_STILL_ALIVE, ownLock.read(resolver));
    }

    @Test
    void shouldRefuseTakeoverWhenCommitFails() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);

        assertFalse(ownLock.takeOver(resolverFailingCommits(1), LOCK_TIMEOUT));

        assertEquals(0L, ownLock.getAttempt());
    }

    @Test
    void shouldRefuseTakeoverWhenAnotherRunTookTheLockFirst() throws Exception {
        lockWrittenBy(OTHER_EXECUTION, 1L, 0L);
        lockOwnedBy(GONE_INSTANCE);
        when(presence.hasLeftCluster(GONE_INSTANCE)).thenReturn(true);
        when(presence.localSlingId()).thenReturn(LOCAL_INSTANCE);
        ExecutionLock fasterRun =
                new ExecutionLock(LOCK_NAME, FASTER_EXECUTION, ALIVE_TIMEOUT, ATTEMPT_LIMIT, presence, ownerJob);
        assertTrue(fasterRun.takeOver(resolver, LOCK_TIMEOUT));

        assertFalse(ownLock.takeOver(resolver, LOCK_TIMEOUT));

        assertEquals(FASTER_EXECUTION, lockNode().get("executionId", String.class));
    }

    /** Lock node left by a run of the given execution. Returns the end of the lock. */
    private Calendar lockWrittenBy(String executionId, Long attempt, long aliveMillisAgo) throws PersistenceException {
        Calendar lockedAt = Calendar.getInstance();
        lockedAt.add(Calendar.MINUTE, -5);
        Calendar lockedUntil = Calendar.getInstance();
        lockedUntil.add(Calendar.MINUTE, 30);
        Calendar alive = Calendar.getInstance();
        alive.setTimeInMillis(alive.getTimeInMillis() - aliveMillisAgo);
        Map<String, Object> props = new HashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("locked", lockedAt);
        props.put("lockedUntil", lockedUntil);
        props.put("executionId", executionId);
        props.put("alive", alive);
        props.put("aliveTimeout", ALIVE_TIMEOUT);
        if (attempt != null) {
            props.put("attempt", attempt);
        }
        ResourceUtil.getOrCreateResource(resolver, Locker.ROOT + "/" + LOCK_NAME, props, "sling:Folder", true);
        return lockedUntil;
    }

    private void lockOwnedBy(String slingId) throws PersistenceException {
        lockNodeChange().put("slingId", slingId);
        resolver.commit();
    }

    private void expiredLockWrittenBy(String executionId) throws PersistenceException {
        lockWrittenBy(executionId, 2L, ONE_MINUTE);
        Resource lock = resolver.getResource(Locker.ROOT + "/" + LOCK_NAME);
        Calendar lockedUntil = Calendar.getInstance();
        lockedUntil.add(Calendar.MINUTE, -1);
        lock.adaptTo(ModifiableValueMap.class).put("lockedUntil", lockedUntil);
        resolver.commit();
    }

    private void lockFolderExists() throws PersistenceException {
        ResourceUtil.getOrCreateResource(
                resolver,
                StringUtils.substringBeforeLast(Locker.ROOT + "/" + LOCK_NAME, "/"),
                "sling:Folder",
                "sling:Folder",
                true);
    }

    /** Another run deletes the lock right after this run checked its state. */
    private ResourceResolver resolverLosingLockAfterCheck() {
        ResourceResolver losing = spy(resolver);
        AtomicInteger lockReads = new AtomicInteger();
        doAnswer(invocation -> lockReads.incrementAndGet() == 1 ? invocation.callRealMethod() : null)
                .when(losing)
                .getResource(Locker.ROOT + "/" + LOCK_NAME);
        return losing;
    }

    /** The same repository, but the first commits fail like during a write conflict. */
    private ResourceResolver resolverFailingCommits(int failingCommits) throws PersistenceException {
        ResourceResolver failing = spy(resolver);
        AtomicInteger commits = new AtomicInteger();
        doAnswer(invocation -> {
                    if (commits.incrementAndGet() <= failingCommits) {
                        throw new PersistenceException("Commit refused");
                    }
                    return invocation.callRealMethod();
                })
                .when(failing)
                .commit();
        return failing;
    }

    private void aliveTimeMovedBack(long millis) throws PersistenceException {
        Calendar alive = Calendar.getInstance();
        alive.setTimeInMillis(alive.getTimeInMillis() - millis);
        lockNodeChange().put("alive", alive);
        resolver.commit();
    }

    private ModifiableValueMap lockNodeChange() {
        return resolver.getResource(Locker.ROOT + "/" + LOCK_NAME).adaptTo(ModifiableValueMap.class);
    }

    private ValueMap lockNode() {
        return resolver.getResource(Locker.ROOT + "/" + LOCK_NAME).getValueMap();
    }

    private static ResourceResolver newResolver() {
        try {
            return MockSling.newResourceResolverFactory(
                            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext())
                    .getServiceResourceResolver(Collections.emptyMap());
        } catch (LoginException e) {
            throw new IllegalStateException(e);
        }
    }
}
