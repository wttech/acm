boolean canRun() {
    return conditions.notQueuedSelf() && conditions.changed()
}

void doRun() {
    throw new RuntimeException("I should run when script content changes or when the instance changes after a failure!")
}
