boolean canRun() {
    return conditions.idleSelf() && conditions.everyWeekAt("Monday", "07:48")
}

void doRun() {
    println("I should run every week!")
}
