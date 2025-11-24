/*
---
version: '1.0'
---
A script that executes when content changes or after deployment failures.
 
This script uses `conditions.changed()` to run when:
- The script content has been modified since last execution, OR
- The script has never been executed before, OR
- The instance state changed (deployment/restart) and previous execution failed

This makes it ideal for deployment scenarios where you want to automatically
retry failed executions after deployments or configuration changes.
*/

boolean canRun() {
    return conditions.changed()
}

void doRun() {
    out.info "Content update task started"
    println("I should run when content changes or after failed deployment!")
    out.success "Content update task completed"
}
