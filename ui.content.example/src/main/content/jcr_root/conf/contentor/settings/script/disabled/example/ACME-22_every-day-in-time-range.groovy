boolean canRun() {
    return condition.idle() && condition.everyDayInTimeRange("10:00", "11:00")
}

void doRun() {
    println("I should run every day between 10:00 and 11:00!")
}
