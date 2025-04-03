boolean canRun() {
    return condition.idleSelf() && condition.everyHourInMinuteRange(30, 35)
}

void doRun() {
    println("I should run every hour between 30 and 35 minutes!")
}
