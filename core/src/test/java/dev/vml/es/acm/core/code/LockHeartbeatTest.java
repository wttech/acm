package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.repo.Locker;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class LockHeartbeatTest {

    private static final String LOCK_NAME = "executor/script/automatic/test.groovy";

    private static final String EXECUTION_ID = "2026/11/18/11/33/job_1";

    private static final long ALIVE_TIMEOUT = 30_000L;

    private static final long ATTEMPT_LIMIT = 2L;

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    private final ExecutionContext context = mock(ExecutionContext.class);

    private final ExecutionLock lock = new ExecutionLock(
            LOCK_NAME, EXECUTION_ID, ALIVE_TIMEOUT, ATTEMPT_LIMIT, new LockOwnerPresence(null), new LockOwnerJob(null));

    @AfterEach
    void clearLogs() {
        TestLoggerFactory.clear();
    }

    @Test
    void shouldKeepRunWhenLockIsOwn() throws Exception {
        lockHeld();
        LockHeartbeat heartbeat = new LockHeartbeat(repository, lock, context);

        heartbeat.run();

        verify(context, never()).stopForLostLock();
    }

    @Test
    void shouldStopRunOnceWhenLockIsNoLongerHeld() {
        LockHeartbeat heartbeat = new LockHeartbeat(repository, lock, context);

        heartbeat.run();
        heartbeat.run();

        verify(context, times(1)).stopForLostLock();
    }

    @Test
    void shouldStopRunForLaterAttemptWhenSameExecutionTookOver() throws Exception {
        lockHeld();
        laterAttemptTookOver();
        LockHeartbeat heartbeat = new LockHeartbeat(repository, lock, context);

        heartbeat.run();

        verify(context).stopForLostLock();
    }

    @Test
    void shouldNotStopRunWhenReleaseStartsDuringBeat() throws Exception {
        lockHeld();
        LockHeartbeat[] heartbeat = new LockHeartbeat[1];
        heartbeat[0] = new LockHeartbeat(releasingDuringBeat(heartbeat), lock, context);

        heartbeat[0].run();

        verify(context, never()).stopForLostLock();
    }

    @Test
    void shouldNotStopRunWhenAliveTimeCannotBeWritten() throws Exception {
        LockHeartbeat heartbeat = new LockHeartbeat(failingRepository(), lock, context);

        heartbeat.run();

        verify(context, never()).stopForLostLock();
    }

    @Test
    void shouldWarnOnceWhenAliveTimeCannotBeWrittenRepeatedly() throws Exception {
        LockHeartbeat heartbeat = new LockHeartbeat(failingRepository(), lock, context);

        heartbeat.run();
        heartbeat.run();

        assertEquals(1, keptAliveFailureWarnings());
    }

    private long keptAliveFailureWarnings() {
        return TestLoggerFactory.getTestLogger(LockHeartbeat.class).getLoggingEvents().stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .filter(event -> event.getMessage().contains("cannot be kept alive"))
                .count();
    }

    private void laterAttemptTookOver() throws Exception {
        ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
        ModifiableValueMap lockNode =
                resolver.getResource(Locker.ROOT + "/" + LOCK_NAME).adaptTo(ModifiableValueMap.class);
        lockNode.put("attempt", 2L);
        resolver.commit();
    }

    private void lockHeld() throws LoginException {
        lock.create(repository.getServiceResourceResolver(Collections.emptyMap()), Duration.ofHours(1));
    }

    /** The run stops the heartbeat and releases its lock while a beat waits for the repository. */
    @SuppressWarnings("unchecked")
    private ResourceResolverFactory releasingDuringBeat(LockHeartbeat[] heartbeat) throws LoginException {
        ResourceResolverFactory releasing = mock(ResourceResolverFactory.class);
        when(releasing.getServiceResourceResolver(any(Map.class))).thenAnswer(invocation -> {
            heartbeat[0].stop();
            ResourceResolver resolver = repository.getServiceResourceResolver(Collections.emptyMap());
            lock.release(resolver);
            return repository.getServiceResourceResolver(Collections.emptyMap());
        });
        return releasing;
    }

    @SuppressWarnings("unchecked")
    private ResourceResolverFactory failingRepository() throws LoginException {
        ResourceResolverFactory failing = mock(ResourceResolverFactory.class);
        when(failing.getServiceResourceResolver(any(Map.class))).thenThrow(new LoginException("Repository stopping"));
        return failing;
    }
}
