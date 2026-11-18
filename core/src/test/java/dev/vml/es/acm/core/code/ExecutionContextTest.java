package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.osgi.OsgiContext;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ExecutionContextTest {

    private static final long STOP_AFTER_UNCONFIRMED_MILLIS = 20_000L;

    private final OsgiContext osgiContext = mock(OsgiContext.class);

    private final ExecutionQueue queue = mock(ExecutionQueue.class);

    @Test
    void shouldKeepTodaysErrorWhenSwitchIsOff() {
        when(osgiContext.getService(ExecutionQueue.class)).thenThrow(new IllegalStateException("Service not found"));
        ExecutionContext context = createContext(false);

        assertThrows(IllegalStateException.class, context::isAborted);
    }

    @Test
    void shouldAbortWithShutdownCauseWhenQueueIsGone() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.empty());
        ExecutionContext context = createContext(true);

        AbortException exception = assertThrows(AbortException.class, context::checkAborted);

        assertEquals(AbortCause.INSTANCE_SHUTDOWN, exception.getAbortCause());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, context.getAbortCause());
    }

    @Test
    void shouldAbortWhenBundleContextIsAlreadyGone() {
        when(osgiContext.findService(ExecutionQueue.class)).thenThrow(new NullPointerException());
        ExecutionContext context = createContext(true);

        assertTrue(context.isAborted());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, context.getAbortCause());
    }

    @Test
    void shouldAbortWhenBundleContextIsInvalid() {
        when(osgiContext.findService(ExecutionQueue.class)).thenThrow(new IllegalStateException("Invalid"));
        ExecutionContext context = createContext(true);

        assertTrue(context.isAborted());
        assertEquals(AbortCause.INSTANCE_SHUTDOWN, context.getAbortCause());
    }

    @Test
    void shouldContinueWhileQueueIsRunning() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        ExecutionContext context = createContext(true);

        assertFalse(context.isAborted());
        assertNull(context.getAbortCause());
    }

    @Test
    void shouldAbortWithoutCauseWhenUserStopsExecution() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        when(queue.isStoppingOrStopped("exec-1")).thenReturn(true);
        ExecutionContext context = createContext(true);

        AbortException exception = assertThrows(AbortException.class, context::checkAborted);

        assertNull(exception.getAbortCause());
        assertNull(context.getAbortCause());
    }

    @Test
    void shouldAbortWithLockLostCauseWhenLockIsHeldByAnotherRun() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        ExecutionContext context = createContext(true);

        context.stopForLostLock();

        AbortException exception = assertThrows(AbortException.class, context::checkAborted);
        assertEquals(AbortCause.EXECUTION_LOCK_LOST, exception.getAbortCause());
        assertEquals(AbortCause.EXECUTION_LOCK_LOST, context.getAbortCause());
    }

    @Test
    void shouldRememberTakeoverByLaterAttempt() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        ExecutionContext context = createContext(true);

        context.stopForLaterAttempt();

        assertTrue(context.isTakenOverByLaterAttempt());
        AbortException exception = assertThrows(AbortException.class, context::checkAborted);
        assertEquals(AbortCause.EXECUTION_LOCK_LOST, exception.getAbortCause());
    }

    @Test
    void shouldAbortWhenLockWasNotConfirmedBeforeOthersMayTreatRunAsKilled() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        ExecutionContext context = createContext(true);

        context.requireLockConfirmation(
                STOP_AFTER_UNCONFIRMED_MILLIS, nanoTimeMillisAgo(STOP_AFTER_UNCONFIRMED_MILLIS + 1_000L));

        assertTrue(context.isAborted());
    }

    @Test
    void shouldContinueWhileLockIsConfirmed() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        ExecutionContext context = createContext(true);

        context.requireLockConfirmation(
                STOP_AFTER_UNCONFIRMED_MILLIS, nanoTimeMillisAgo(STOP_AFTER_UNCONFIRMED_MILLIS / 4));

        assertFalse(context.isAborted());
    }

    @Test
    void shouldContinueAgainWhenLateBeatConfirmsLock() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.of(queue));
        ExecutionContext context = createContext(true);
        context.requireLockConfirmation(
                STOP_AFTER_UNCONFIRMED_MILLIS, nanoTimeMillisAgo(STOP_AFTER_UNCONFIRMED_MILLIS + 1_000L));
        context.isAborted();

        context.confirmLockHeldAt(System.nanoTime());

        assertFalse(context.isAborted());
    }

    @Test
    void shouldReportShutdownBeforeUnconfirmedLock() {
        when(osgiContext.findService(ExecutionQueue.class)).thenReturn(Optional.empty());
        ExecutionContext context = createContext(true);
        context.requireLockConfirmation(
                STOP_AFTER_UNCONFIRMED_MILLIS, nanoTimeMillisAgo(STOP_AFTER_UNCONFIRMED_MILLIS + 1_000L));

        AbortException exception = assertThrows(AbortException.class, context::checkAborted);

        assertEquals(AbortCause.INSTANCE_SHUTDOWN, exception.getAbortCause());
    }

    private long nanoTimeMillisAgo(long millis) {
        return System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(millis);
    }

    private ExecutionContext createContext(boolean abortOnShutdown) {
        CodeContext codeContext = mock(CodeContext.class, RETURNS_DEEP_STUBS);
        when(codeContext.getOsgiContext()).thenReturn(osgiContext);
        Executable executable = mock(Executable.class);
        when(executable.getId()).thenReturn("/conf/acm/settings/script/automatic/test.groovy");

        ExecutionContext context = new ExecutionContext(
                "exec-1",
                "acm-content-service",
                ExecutionMode.RUN,
                mock(Executor.class),
                executable,
                new InputValues(),
                codeContext,
                new CodeOutputMemory());
        context.setAbortOnShutdown(abortOnShutdown);
        return context;
    }
}
