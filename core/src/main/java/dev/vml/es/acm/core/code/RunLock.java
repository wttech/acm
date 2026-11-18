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
}
