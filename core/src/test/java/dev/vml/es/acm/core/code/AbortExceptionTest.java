package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AbortExceptionTest {

    @Test
    void shouldCarryShutdownCause() {
        AbortException exception = new AbortException(AbortCause.INSTANCE_SHUTDOWN);

        assertEquals(AbortCause.INSTANCE_SHUTDOWN, exception.getAbortCause());
    }

    @Test
    void shouldHaveNoCauseForUserAbort() {
        assertNull(new AbortException("stopped").getAbortCause());
    }
}
