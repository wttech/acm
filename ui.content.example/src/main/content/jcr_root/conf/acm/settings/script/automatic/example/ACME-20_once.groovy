/*
A simple demonstration script that executes only once.
 
This script uses `conditions.once()` to ensure it runs exactly one time in the instance's lifetime.
Useful for one-time initialization tasks that should never be repeated even if failed.
*/

boolean canRun() {
    return conditions.once()
}

void doRun() {
    out.info "One-time task started"
    println("I should run only once!")
    out.success "One-time task completed"
}
