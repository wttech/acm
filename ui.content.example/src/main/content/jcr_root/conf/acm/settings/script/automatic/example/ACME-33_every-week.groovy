def scheduleRun() {
    return schedules.cron("0 48 7 ? * MON *") // at 07:48 every Monday
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println("I should run every week!")
}
