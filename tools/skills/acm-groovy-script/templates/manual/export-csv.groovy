/*
---
version: '1.0'
category: report
tags: ['assets']
---
Exports data as a downloadable CSV file, e.g. for reports and audits.

Exports path, title and format of all assets below a root path.
*/

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
