/*
---
version: '1.0'
category: migration
tags: ['content']
---
Imports data from an uploaded CSV file, e.g. to update content in bulk from a spreadsheet.

Sets properties on resources from a CSV with the columns `path,property,value` and a header row. Values may be quoted, but not span lines. Preview with dry run first. The uploaded file is removed afterwards.
*/

void describeRun() {
    inputs.file('csv') {
        label = 'CSV file'
        description = 'Columns: path,property,value'
        mimeTypes = ['text/csv', 'application/vnd.ms-excel']
    }
    inputs.bool('dryRun') { value = true; switcher(); description = 'Preview changes without saving' }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def file = repo.get(inputs.value('csv'))
    def dryRun = inputs.value('dryRun')
    log.info "CSV import started | file=${file.path}, dryRun=${dryRun}"

    def updated = 0
    def skipped = 0
    try {
        repo.dryRun(dryRun) {
            file.readFileAsString().readLines().drop(1).each { line ->
                context.checkAborted()
                if (!line.trim()) {
                    return
                }
                def (path, property, value) = parseCsvLine(line)
                def resource = path && property ? repo.get(path) : null
                if (resource == null || !resource.exists()) {
                    log.warn "Skipped | line=${line}"
                    skipped++
                    return
                }
                resource.saveProperty(property, value)
                updated++
            }
        }
    } finally {
        file.delete()
    }

    outputs.text('summary') { label = 'Summary'; value = "Updated ${updated}, skipped ${skipped}." }
    log.info "CSV import finished | updated=${updated}, skipped=${skipped}, dryRun=${dryRun}"
}

List<String> parseCsvLine(String line) {
    return line.split(/,(?=(?:[^"]*"[^"]*")*[^"]*$)/, -1).collect { it.trim().replaceAll(/^"|"$/, '').replace('""', '"') }
}
