/*
---
version: '1.0'
author: jane.doe@acme.com
category: general
tags: []
---
A starting point for any script run on demand.

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
