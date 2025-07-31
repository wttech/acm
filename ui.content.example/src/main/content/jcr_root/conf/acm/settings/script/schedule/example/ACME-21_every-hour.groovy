boolean canRun() {
    return conditions.everyHourAt(43)
}

void doRun() {
    println("I should run every hour!")
}
