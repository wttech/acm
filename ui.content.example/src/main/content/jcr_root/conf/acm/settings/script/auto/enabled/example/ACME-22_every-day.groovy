boolean canRun() {
    return conditions.notQueuedSelf() && conditions.everyDayAt("07:45")
}

void doRun() {
    println("I should run every day!")
}
