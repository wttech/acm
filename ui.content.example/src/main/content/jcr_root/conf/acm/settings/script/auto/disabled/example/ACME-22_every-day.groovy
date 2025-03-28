boolean canRun() {
    return condition.idle() && condition.everyDay()
}

void doRun() {
    println("I should run every day!")
}
