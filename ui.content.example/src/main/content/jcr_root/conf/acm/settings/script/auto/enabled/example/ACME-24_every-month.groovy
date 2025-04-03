boolean canRun() {
    return condition.idleSelf() && condition.everyMonth()
}

void doRun() {
    println("I should run every month!")
}
