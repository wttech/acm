/*
---
version: '1.0'
category: migration
tags: ['content']
---
Updates content in bulk, with the scope given as inputs.

Replaces a property value on all resources of a resource type below a root path. Input values end up in the query, so only trusted users should run it.
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
