boolean canRun() {
    return conditions.notQueuedSelf() && conditions.once()
}

void doRun() {
    println("I should run only once!")
}
