boolean canRun() {
    return conditions.idleSelf() && conditions.everyDay("07:45")
}

void doRun() {
    println("I should run every day!")
}
