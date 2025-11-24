/*
A scheduled script that runs monthly on the 21st day at 07:49.
 
This script demonstrates monthly cron-based scheduling.
*/

def scheduleRun() {
    return schedules.cron("0 49 7 21 * ? *") // at 07:49 on the 21st day of every month
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    out.info "Monthly task started"
    println("I should run every month!")
    out.success "Monthly task completed"
}
