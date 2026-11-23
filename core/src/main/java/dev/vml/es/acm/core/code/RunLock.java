package dev.vml.es.acm.core.code;

interface RunLock {

    /** For health checks, which use no lock. */
    RunLock NONE = new RunLock() {
        @Override
        public boolean enter() {
            return true;
        }

        @Override
        public void hold() {}

        @Override
        public boolean isHeld() {
            return true;
        }

        @Override
        public void release() {}
    };

    /** False when another run holds the lock. */
    boolean enter();

    void hold();

    /** False when another run took the lock during the run. */
    boolean isHeld();

    void release();

    /** True when a later attempt of the same execution took the lock and reports the result instead. */
    default boolean isTakenOverByLaterAttempt() {
        return false;
    }

    /** True when an earlier attempt of the same execution refused the run, so the shared history entry is not this run's. */
    default boolean isRefusedByOwnEarlierRun() {
        return false;
    }

    /** True when the run could not release its lock, so Sling must keep the job as a trace for a later takeover. */
    default boolean isLeftBehind() {
        return false;
    }
}
