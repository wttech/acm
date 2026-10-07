/*
---
version: '1.0'
author: jane.doe@acme.com
category: setup
tags: ['config']
---
Describe what the script sets up. Re-applied whenever this script changes.
*/

boolean canRun() {
    return conditions.changed() && conditions.isInstanceAuthor()
}

void doRun() {
    log.info "Setup started"
    // idempotent changes only
    log.info "Setup finished"
}
