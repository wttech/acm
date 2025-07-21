boolean canRun() {
    return condition.idleSelf() && condition.everyMonth(21, "07:49")
}

void doRun() {
    println("I should run every month!")
}
