package dev.vml.es.acm.core.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Calendar;
import org.junit.jupiter.api.Test;

class LockerTest {

    @Test
    void shouldEndLockAfterItsTimeToLive() {
        long earliestEnd = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();

        Calendar lockedUntil = Locker.lockedUntil(Duration.ofMinutes(10));

        long latestEnd = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
        assertTrue(lockedUntil.getTimeInMillis() >= earliestEnd);
        assertTrue(lockedUntil.getTimeInMillis() <= latestEnd);
    }

    @Test
    void shouldNeverEndLockWithoutPositiveTimeToLive() {
        assertNull(Locker.lockedUntil(null));
        assertNull(Locker.lockedUntil(Duration.ZERO));
        assertNull(Locker.lockedUntil(Duration.ofMinutes(-1)));
    }
}
