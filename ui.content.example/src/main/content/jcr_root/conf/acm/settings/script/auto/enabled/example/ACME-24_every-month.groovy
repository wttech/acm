boolean canRun() {
    return condition.idleSelf() && condition.everyMonth(3, "15:00")
}

void doRun() {
    println("I should run every month!")
}
