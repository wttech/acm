boolean canRun() {
    return conditions.idleSelf() && conditions.everyDayAt("07:45")
}

void doRun() {
    println("I should run every day!")
}
