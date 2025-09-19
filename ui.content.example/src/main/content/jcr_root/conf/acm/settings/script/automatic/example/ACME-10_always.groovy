def scheduleRun() {
    return schedules.none(); // schedules.boot()
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println("I should run always on boot!")
}
