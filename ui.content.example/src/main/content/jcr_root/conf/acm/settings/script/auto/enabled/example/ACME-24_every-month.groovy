boolean canRun() {
    return conditions.idleSelf() && conditions.everyMonth(21, "07:49")
}

void doRun() {
    println("I should run every month!")
}
