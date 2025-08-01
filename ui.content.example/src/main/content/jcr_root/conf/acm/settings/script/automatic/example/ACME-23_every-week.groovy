boolean canRun() {
    return conditions.everyWeekAt("Monday", "07:48")
}

void doRun() {
    println("I should run every week!")
}
