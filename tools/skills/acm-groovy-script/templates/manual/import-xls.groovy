/*
---
version: '1.0'
category: migration
tags: ['content']
---
Imports data from an uploaded Excel workbook, e.g. to update content in bulk from a spreadsheet maintained by business users.

Sets properties on resources from the first sheet of an XLSX file with the columns `path`, `property` and `value` and a header row, read with Apache POI. Preview with dry run first. The uploaded file is removed afterwards.
*/

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook

void describeRun() {
    inputs.file('xls') {
        label = 'XLSX file'
        description = 'First sheet with the columns: path, property, value'
        mimeTypes = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']
    }
    inputs.bool('dryRun') { value = true; switcher(); description = 'Preview changes without saving' }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def file = repo.get(inputs.value('xls'))
    def dryRun = inputs.value('dryRun')
    log.info "XLS import started | file=${file.path}, dryRun=${dryRun}"

    def formatter = new DataFormatter()
    def updated = 0
    def skipped = 0
    def workbook = file.readFileAsStream().withStream { new XSSFWorkbook(it) }
    try {
        repo.dryRun(dryRun) {
            workbook.getSheetAt(0).each { row ->
                if (row.rowNum == 0) {
                    return
                }
                context.checkAborted()
                def (path, property, value) = (0..2).collect { formatter.formatCellValue(row.getCell(it)) }
                def resource = path && property ? repo.get(path) : null
                if (resource == null || !resource.exists()) {
                    log.warn "Skipped | row=${row.rowNum + 1}"
                    skipped++
                    return
                }
                resource.saveProperty(property, value)
                updated++
            }
        }
    } finally {
        workbook.close()
        file.delete()
    }

    outputs.text('summary') { label = 'Summary'; value = "Updated ${updated}, skipped ${skipped}." }
    log.info "XLS import finished | updated=${updated}, skipped=${skipped}, dryRun=${dryRun}"
}
