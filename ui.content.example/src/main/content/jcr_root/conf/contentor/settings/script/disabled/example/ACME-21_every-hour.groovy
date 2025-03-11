boolean canRun() {
    return condition.idle() && condition.everyHour()
}

void doRun() {
    println("I should run every hour!")
}
