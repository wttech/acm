boolean canRun() {
    return condition.idleSelf() && condition.everyDay("07:45")
}

void doRun() {
    println("I should run every day!")
}
