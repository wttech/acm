boolean canRun() {
    return condition.idleSelf() && condition.everyDay()
}

void doRun() {
    println("I should run every day!")
}
