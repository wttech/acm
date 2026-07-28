package dev.vml.es.acm.core.repo;

import java.util.Calendar;

public class LockInfo {

    private final Calendar lockedAt;

    private final Calendar lockedUntil;

    public LockInfo(Calendar lockedAt, Calendar lockedUntil) {
        this.lockedAt = lockedAt;
        this.lockedUntil = lockedUntil;
    }

    public Calendar getLockedAt() {
        return lockedAt;
    }

    public Calendar getLockedUntil() {
        return lockedUntil;
    }

    public boolean isExpired() {
        return lockedUntil != null && Calendar.getInstance().after(lockedUntil);
    }
}
