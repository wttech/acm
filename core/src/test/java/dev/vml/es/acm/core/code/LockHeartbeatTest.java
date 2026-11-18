package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LockHeartbeatTest {

    private static final String LOCK_NAME = "executor/script/automatic/test.groovy";

    private static final String EXECUTION_ID = "2026/11/18/11/33/job_1";

    private static final long ALIVE_TIMEOUT = 30_000L;

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    private final ExecutionContext context = mock(ExecutionContext.class);

    private final ExecutionLock lock = new ExecutionLock(LOCK_NAME, EXECUTION_ID, ALIVE_TIMEOUT);

    @Test
    void shouldConfirmLockWithTimeTakenBeforeTheWrite() throws Exception {
        lockHeld();
        LockHeartbeat heartbeat = new LockHeartbeat(repository, lock, context);
        long beforeBeat = System.nanoTime();

        heartbeat.run();

        ArgumentCaptor<Long> confirmedAt = ArgumentCaptor.forClass(Long.class);
        verify(context).confirmLockHeldAt(confirmedAt.capture());
        assertTrue(confirmedAt.getValue() >= beforeBeat);
        assertTrue(confirmedAt.getValue() <= System.nanoTime());
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

        verify(context).stopForLaterAttempt();
        verify(context, never()).stopForLostLock();
    }

    @Test
    void shouldNotConfirmLockWhenAliveTimeCannotBeWritten() throws Exception {
        LockHeartbeat heartbeat = new LockHeartbeat(failingRepository(), lock, context);

        heartbeat.run();

        verify(context, never()).confirmLockHeldAt(anyLong());
        verify(context, never()).stopForLostLock();
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

    @SuppressWarnings("unchecked")
    private ResourceResolverFactory failingRepository() throws LoginException {
        ResourceResolverFactory failing = mock(ResourceResolverFactory.class);
        when(failing.getServiceResourceResolver(any(Map.class))).thenThrow(new LoginException("Repository stopping"));
        return failing;
    }
}
