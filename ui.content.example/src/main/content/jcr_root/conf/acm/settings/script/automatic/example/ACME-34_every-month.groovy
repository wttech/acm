def scheduleRun() {
    return schedules.cron("0 49 7 21 * ? *") // at 07:49 on the 21st day of every month
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println("I should run every month!")
}
