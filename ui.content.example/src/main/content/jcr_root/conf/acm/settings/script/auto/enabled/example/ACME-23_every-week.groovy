boolean canRun() {
    return conditions.notQueuedSelf() && conditions.everyWeekAt("Monday", "07:48")
}

void doRun() {
    println("I should run every week!")
}
