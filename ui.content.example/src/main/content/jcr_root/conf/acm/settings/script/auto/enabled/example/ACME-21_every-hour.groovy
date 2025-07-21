boolean canRun() {
    return condition.idleSelf() && condition.everyHour(43)
}

void doRun() {
    println("I should run every hour!")
}
