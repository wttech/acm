/*
---
version: '1.0'
author: jane.doe@acme.com
category: setup
tags: ['config']
---
Applies a setup once and again whenever the script changes.

Describe what the script sets up.
*/

boolean canRun() {
    return conditions.changed()
}

void doRun() {
    log.info "Setup started"
    // idempotent changes only
    log.info "Setup finished"
}
