package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.osgi.OsgiContext;
import org.junit.jupiter.api.Test;

class ExecutionContextTest {

    private final OsgiContext osgiContext = mock(OsgiContext.class);

    private final ExecutionQueue queue = mock(ExecutionQueue.class);

    @Test
    void shouldThrowWhenQueueIsGone() {
        when(osgiContext.getService(ExecutionQueue.class)).thenThrow(new IllegalStateException("Service not found"));
        ExecutionContext context = createContext();

        assertThrows(IllegalStateException.class, context::isAborted);
    }

    @Test
    void shouldContinueWhileQueueIsRunning() {
        when(osgiContext.getService(ExecutionQueue.class)).thenReturn(queue);
        ExecutionContext context = createContext();

        assertFalse(context.isAborted());
        assertFalse(context.isLockLost());
    }

    @Test
    void shouldAbortGracefullyWhenUserStopsExecution() {
        when(osgiContext.getService(ExecutionQueue.class)).thenReturn(queue);
        when(queue.isStoppingOrStopped("exec-1")).thenReturn(true);
        ExecutionContext context = createContext();

        assertThrows(AbortException.class, context::checkAborted);

        assertFalse(context.isLockLost());
    }

    @Test
    void shouldAbortForLostLockWhenLockIsHeldByAnotherRun() {
        when(osgiContext.getService(ExecutionQueue.class)).thenReturn(queue);
        ExecutionContext context = createContext();

        context.stopForLostLock();

        AbortException exception = assertThrows(AbortException.class, context::checkAborted);
        assertEquals(ExecutionContext.LOCK_LOST_MESSAGE, exception.getMessage());
        assertTrue(context.isLockLost());
    }

    private ExecutionContext createContext() {
        CodeContext codeContext = mock(CodeContext.class, RETURNS_DEEP_STUBS);
        when(codeContext.getOsgiContext()).thenReturn(osgiContext);
        Executable executable = mock(Executable.class);
        when(executable.getId()).thenReturn("/conf/acm/settings/script/automatic/test.groovy");

        return new ExecutionContext(
                "exec-1",
                "acm-content-service",
                ExecutionMode.RUN,
                mock(Executor.class),
                executable,
                new InputValues(),
                codeContext,
                new CodeOutputMemory());
    }
}
