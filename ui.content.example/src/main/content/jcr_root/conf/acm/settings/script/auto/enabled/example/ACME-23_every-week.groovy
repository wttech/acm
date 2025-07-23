boolean canRun() {
    return conditions.idleSelf() && conditions.everyWeek("Monday", "07:48")
}

void doRun() {
    println("I should run every week!")
}
