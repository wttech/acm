/**
 * A scheduled script that runs daily at 08:00.
 * 
 * This script demonstrates cron-based scheduling with `schedules.cron()` and uses
 * `conditions.always()` to execute on every scheduled trigger.
 */

def scheduleRun() {
    return schedules.cron("0 0 8 ? * * *") // at 08:00 every day
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    out.info "Daily task started"
    println("I should run every day!")
    out.success "Daily task completed"
}
