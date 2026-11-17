package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AbortCauseTest {

    @Test
    void shouldReadStoredName() {
        assertEquals(Optional.of(AbortCause.INSTANCE_SHUTDOWN), AbortCause.of("INSTANCE_SHUTDOWN"));
    }

    @Test
    void shouldReadNothingWhenNoCauseIsStored() {
        assertEquals(Optional.empty(), AbortCause.of(null));
    }

    @Test
    void shouldReadNothingForUnknownName() {
        assertEquals(Optional.empty(), AbortCause.of("UNKNOWN"));
    }
}
