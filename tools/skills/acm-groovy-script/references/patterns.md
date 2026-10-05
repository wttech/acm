# Patterns

Complete, working templates. Adapt names, paths and resource types; keep the safety parts (dry run, abort checks, logging).

## Content migration: update a property

Manual script with inputs, dry run on by default.

```groovy
/*
---
version: '1.0'
category: migration
tags: ['content']
---
Replaces a property value on all resources of a resource type below a root path.
*/

void describeRun() {
    inputs.path('rootPath') { value = '/content/acme'; description = 'Root path to search below' }
    inputs.string('resourceType') { description = 'sling:resourceType to match, e.g. acme/components/teaser' }
    inputs.string('propertyName') { description = 'Property to update' }
    inputs.string('oldValue') { description = 'Current value' }
    inputs.string('newValue') { description = 'New value' }
    inputs.bool('dryRun') { value = true; switcher(); description = 'Preview changes without saving' }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def rootPath = inputs.value('rootPath')
    def resourceType = inputs.value('resourceType')
    def propertyName = inputs.value('propertyName')
    def oldValue = inputs.value('oldValue')
    def newValue = inputs.value('newValue')
    def dryRun = inputs.value('dryRun')
    log.info "Property update started | root=${rootPath}, type=${resourceType}, ${propertyName}: '${oldValue}' -> '${newValue}', dryRun=${dryRun}"

    def scanned = 0
    def updated = 0
    repo.dryRun(dryRun) {
        repo.get(rootPath).query('nt:base', "n.[sling:resourceType] = '${resourceType}'").forEach { resource ->
            context.checkAborted()
            scanned++
            if (resource.property(propertyName, String) == oldValue) {
                resource.saveProperty(propertyName, newValue)
                updated++
                log.debug "Updated ${resource.path}"
            }
            if (scanned % 500 == 0) {
                out.info "Scanned ${scanned}, updated ${updated}"
            }
        }
    }

    log.info "Property update finished | scanned=${scanned}, updated=${updated}, dryRun=${dryRun}"
}
```

To remove a property instead, call `resource.deleteProperty(propertyName)`. Values from inputs end up in the query, so only run this as a trusted user.

## Permissions: project users and groups

Automatic script, re-applied whenever it changes.

```groovy
/*
---
version: '1.0'
category: security
tags: ['acl']
---
Creates the ACME service user and authors group with their permissions.
*/

boolean canRun() {
    return conditions.changed() && conditions.isInstanceAuthor()
}

void doRun() {
    log.info "ACL setup started"

    def service = acl.createUser { id = 'acme.service'; systemUser(); skipIfExists() }
    service.allow { path = '/content/acme'; permissions = ['jcr:read', 'rep:write'] }
    service.allow { path = '/content/dam/acme'; permissions = ['jcr:read'] }

    def authors = acl.createGroup { id = 'acme-authors'; skipIfExists() }
    authors.allow { path = '/content/acme'; permissions = ['jcr:read', 'rep:write', 'crx:replicate'] }
    authors.deny { path = '/content/acme/admin'; permissions = ['rep:write'] }
    authors.addMember(service)

    log.info "ACL setup finished"
}
```

## Report: CSV export

Manual script producing a downloadable file and a summary.

```groovy
void describeRun() {
    inputs.path('rootPath') { value = '/content/dam/acme'; description = 'Assets root' }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def rootPath = inputs.value('rootPath')
    log.info "Asset report started | root=${rootPath}"

    def report = outputs.file('report') {
        label = 'Assets'
        description = "Assets below ${rootPath}"
        downloadName = 'assets.csv'
    }
    report.out.println('path,title,format')

    def count = 0
    repo.get(rootPath).query('dam:Asset').forEach { asset ->
        context.checkAborted()
        def title = asset.property('jcr:content/metadata/dc:title', String) ?: ''
        def format = asset.property('jcr:content/metadata/dc:format', String) ?: ''
        report.out.println([asset.path, title, format].collect { "\"${it.replace('"', '""')}\"" }.join(','))
        count++
    }

    outputs.text('summary') { label = 'Summary'; value = "Exported ${count} assets." }
    log.info "Asset report finished | assets=${count}"
}
```

## Maintenance: scheduled cleanup

Automatic script running nightly.

```groovy
/*
---
version: '1.0'
schedule: Daily at 3:00
category: maintenance
---
Deletes temporary data older than 7 days.
*/

def scheduleRun() {
    return schedules.cron('0 0 3 ? * * *')
}

boolean canRun() {
    return conditions.always() && conditions.isInstanceAuthor()
}

void doRun() {
    def cutoff = java.time.LocalDateTime.now().minusDays(7)
    log.info "Cleanup started | cutoff=${cutoff}"

    def deleted = 0
    repo.get('/var/acme/tmp').children().toList().each { resource ->
        context.checkAborted()
        def created = resource.property('jcr:created', java.time.LocalDateTime)
        if (created && created.isBefore(cutoff)) {
            resource.delete()
            deleted++
        }
    }

    log.info "Cleanup finished | deleted=${deleted}"
}
```

Children are collected with `toList()` first, so deleting does not change the sequence being iterated.

## Console: quick read-only check

```groovy
def pages = repo.get('/content/acme/en').query('cq:Page').toList()
println "Pages: ${pages.size()}"
pages.take(10).each { println it.path }
```
