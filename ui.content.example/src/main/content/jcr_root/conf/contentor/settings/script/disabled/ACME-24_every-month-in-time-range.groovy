boolean canRun() {
    return condition.everyMonthInTimeRange("10:00", "11:00")
}

void doRun() {
    println("I should run every month between 10:00 and 11:00!")
}
