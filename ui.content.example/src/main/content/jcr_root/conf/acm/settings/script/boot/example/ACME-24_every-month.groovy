boolean canRun() {
    return conditions.notQueuedSelf() && conditions.everyMonthAt(21, "07:49")
}

void doRun() {
    println("I should run every month!")
}
