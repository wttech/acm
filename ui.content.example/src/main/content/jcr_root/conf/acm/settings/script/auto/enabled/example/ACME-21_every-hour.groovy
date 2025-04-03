boolean canRun() {
    return condition.idleSelf() && condition.everyHour()
}

void doRun() {
    println("I should run every hour!")
}
