/*
A scheduled script that runs weekly every Monday at 07:48.
 
This script demonstrates weekly cron-based scheduling and uses `conditions.always()` to ensure execution on every scheduled trigger.
*/

def scheduleRun() {
    return schedules.cron("0 48 7 ? * MON *") // at 07:48 every Monday
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    out.info "Weekly task started"
    println("I should run every week!")
    out.success "Weekly task completed"
}
