boolean canRun() {
    return conditions.idleSelf() && conditions.everyHour(43)
}

void doRun() {
    println("I should run every hour!")
}
