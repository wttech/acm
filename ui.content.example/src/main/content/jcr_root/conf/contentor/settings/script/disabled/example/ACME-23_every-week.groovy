boolean canRun() {
    return condition.idle() && condition.everyWeek()
}

void doRun() {
    println("I should run every week!")
}
