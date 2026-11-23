package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

class OwnedRunLockTest {

    private static final String LOCK_NAME = "executor/script/automatic/test.groovy";

    private static final String EXECUTION_ID = "2026/11/18/11/33/job_1";

    // beats every 100 ms, so the release waits for a beat only that long
    private static final long ALIVE_TIMEOUT_MILLIS = 600L;

    private static final long ATTEMPT_LIMIT = 2L;

    private final ResourceResolverFactory repository = MockSling.newResourceResolverFactory(
            ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    private final ExecutionContext context = mock(ExecutionContext.class, RETURNS_DEEP_STUBS);

    private final CountDownLatch beatWaitsForRepository = new CountDownLatch(1);

    private final CountDownLatch lockReleased = new CountDownLatch(1);

    @Test
    void shouldNotStopRunWhenBeatInFlightEndsAfterRelease() throws Exception {
        when(context.getId()).thenReturn(EXECUTION_ID);
        ExecutionLock lock = new ExecutionLock(
                LOCK_NAME,
                EXECUTION_ID,
                ALIVE_TIMEOUT_MILLIS,
                ATTEMPT_LIMIT,
                new LockOwnerPresence(null),
                new LockOwnerJob(null));
        OwnedRunLock runLock = new OwnedRunLock(repositoryHoldingFirstBeat(), context, lock, () -> Duration.ofHours(1));
        assertTrue(runLock.enter());
        runLock.hold();
        assertTrue(beatWaitsForRepository.await(5, TimeUnit.SECONDS));

        runLock.release();
        lockReleased.countDown();

        verify(context, after(500).never()).stopForLostLock();
    }

    /** Real in-memory repository; the first heartbeat waits in it until the run released its lock. */
    @SuppressWarnings("unchecked")
    private ResourceResolverFactory repositoryHoldingFirstBeat() throws LoginException {
        AtomicBoolean firstBeat = new AtomicBoolean(true);
        ResourceResolverFactory holding = mock(ResourceResolverFactory.class);
        when(holding.getServiceResourceResolver(any(Map.class))).thenAnswer(invocation -> {
            if (Thread.currentThread().getName().startsWith("ACM execution lock heartbeat")
                    && firstBeat.getAndSet(false)) {
                beatWaitsForRepository.countDown();
                lockReleased.await(5, TimeUnit.SECONDS);
            }
            return repository.getServiceResourceResolver(invocation.getArgument(0));
        });
        return holding;
    }
}
