boolean canRun() {
    return condition.hourlyInMinuteRange(30, 35)
}

void doRun() {
    println("I should run every hour between 30 and 35 minutes!")
}
