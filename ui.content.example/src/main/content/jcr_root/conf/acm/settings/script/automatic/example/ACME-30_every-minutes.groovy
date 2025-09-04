def scheduleRun() {
    return schedules.cron("0 0/30 * * * ?")
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println("I should run every 30 minutes!")
}
