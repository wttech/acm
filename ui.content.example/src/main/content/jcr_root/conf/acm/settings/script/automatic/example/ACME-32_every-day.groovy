def scheduleRun() {
    return schedules.cron("0 0 8 ? * * *") // at 08:00 every day
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println("I should run every day!")
}
