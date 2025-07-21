boolean canRun() {
    return condition.idleSelf() && condition.everyDay("09:08")
}

void doRun() {
    println("I should run every day!")
}
