/*
---
version: '1.0'
category: report
tags: ['assets']
---
Exports data as a downloadable Excel workbook, e.g. for business reports.

Exports path, title and format of all assets below a root path as an XLSX file, written with Apache POI. The workbook is built in memory, so prefer a CSV for very large exports.
*/

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

void describeRun() {
    inputs.path('rootPath') { value = '/content/dam/acme'; description = 'Assets root' }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    def rootPath = inputs.value('rootPath')
    log.info "Asset workbook started | root=${rootPath}"

    def count = 0
    Workbook workbook = new XSSFWorkbook()
    try {
        def sheet = workbook.createSheet('Assets')
        def header = sheet.createRow(0)
        ['Path', 'Title', 'Format'].eachWithIndex { name, column -> header.createCell(column).setCellValue(name) }

        repo.get(rootPath).query('dam:Asset').forEach { asset ->
            context.checkAborted()
            def row = sheet.createRow(++count)
            row.createCell(0).setCellValue(asset.path)
            row.createCell(1).setCellValue(asset.property('jcr:content/metadata/dc:title', String) ?: '')
            row.createCell(2).setCellValue(asset.property('jcr:content/metadata/dc:format', String) ?: '')
        }
        (0..2).each { sheet.autoSizeColumn(it) }

        def report = outputs.file('report') {
            label = 'Assets'
            description = "Assets below ${rootPath}"
            downloadName = 'assets.xlsx'
        }
        workbook.write(report.outputStream)
    } finally {
        workbook.close()
    }

    outputs.text('summary') { label = 'Summary'; value = "Exported ${count} assets." }
    log.info "Asset workbook finished | assets=${count}"
}
