boolean canRun() {
    return conditions.idleSelf() && conditions.everyHourAt(43)
}

void doRun() {
    println("I should run every hour!")
}
