boolean canRun() {
    return condition.hourly()
}

void doRun() {
    println("I should run every hour!")
}
