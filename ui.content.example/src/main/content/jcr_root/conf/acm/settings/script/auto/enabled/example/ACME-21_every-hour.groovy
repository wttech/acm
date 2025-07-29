boolean canRun() {
    return conditions.notQueuedSelf() && conditions.everyHourAt(43)
}

void doRun() {
    println("I should run every hour!")
}
