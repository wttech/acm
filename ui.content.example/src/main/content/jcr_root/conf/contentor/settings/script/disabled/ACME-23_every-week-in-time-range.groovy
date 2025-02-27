boolean canRun() {
    return condition.everyWeekInTimeRange("10:00", "11:00")
}

void doRun() {
    println("I should run every week between 10:00 and 11:00!")
}
