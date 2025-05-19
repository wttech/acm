package dev.vml.es.acm.core.instance;

import java.io.Serializable;

public class SlingInstallerState implements Serializable {

    private final long pauseCount;

    private final long activeResourceCount;

    private final boolean active;

    public SlingInstallerState(boolean active, long activeResourceCount, long pauseCount) {
        this.active = active;
        this.activeResourceCount = activeResourceCount;
        this.pauseCount = pauseCount;
    }

    public boolean isIdle() {
        return !active && pauseCount == 0;
    }

    public boolean isBusy() {
        return !isIdle();
    }

    public boolean isPaused() {
        return pauseCount > 0;
    }

    public long getPauseCount() {
        return pauseCount;
    }

    public boolean isActive() {
        return active;
    }

    public long getActiveResourceCount() {
        return activeResourceCount;
    }
}
