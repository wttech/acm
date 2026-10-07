/*
---
version: '1.0'
schedule: Daily at 3:00
category: maintenance
---
Deletes temporary data older than 7 days.
*/

import java.time.LocalDateTime

def scheduleRun() {
    return schedules.cron('0 0 3 ? * * *')
}

boolean canRun() {
    return conditions.always() && conditions.isInstanceAuthor()
}

void doRun() {
    def cutoff = LocalDateTime.now().minusDays(7)
    log.info "Cleanup started | cutoff=${cutoff}"

    def deleted = 0
    // toList() first, so deleting does not change the sequence being iterated
    repo.get('/var/acme/tmp').children().toList().each { resource ->
        context.checkAborted()
        def created = resource.property('jcr:created', LocalDateTime)
        if (created && created.isBefore(cutoff)) {
            resource.delete()
            deleted++
        }
    }

    log.info "Cleanup finished | deleted=${deleted}"
}
