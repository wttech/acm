boolean canRun() {
    return condition.idleSelf() && condition.everyWeek()
}

void doRun() {
    println("I should run every week!")
}
