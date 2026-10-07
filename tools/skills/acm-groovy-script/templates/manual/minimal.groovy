/*
---
version: '1.0'
author: jane.doe@acme.com
category: general
tags: []
---
Describe what the script does, where and why.
*/

boolean canRun() {
    return conditions.always()
}

void doRun() {
    log.info "Started"
    // work
    log.info "Finished"
}
