boolean canRun() {
    return condition.idle() && condition.everyMonth()
}

void doRun() {
    println("I should run every month!")
}
