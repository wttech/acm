boolean canRun() {
    return condition.idleSelf() && condition.everyWeek("Monday", "07:48")
}

void doRun() {
    println("I should run every week!")
}
