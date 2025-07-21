boolean canRun() {
    return condition.idleSelf() && condition.everyHour(7)
}

void doRun() {
    println("I should run every hour!")
}
