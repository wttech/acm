def scheduleRun() {
    return schedules.cron("0 0 * ? * * *")
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println("I should run every hour!")
}


