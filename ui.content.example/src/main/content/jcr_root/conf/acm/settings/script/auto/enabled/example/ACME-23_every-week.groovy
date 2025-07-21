boolean canRun() {
    return condition.idleSelf() && condition.everyWeek("Wednesday", "14:00")
}

void doRun() {
    println("I should run every week!")
}
