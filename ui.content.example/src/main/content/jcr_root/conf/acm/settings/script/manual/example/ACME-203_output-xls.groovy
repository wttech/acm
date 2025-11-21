import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Random

/**
  * @description Generates an XLS report of users with random names and birth dates.
  * @author <john.doe@acme.com>
  */
void describeRun() {
  inputs.integerNumber("count") { label = "Users to generate"; min = 1; value = 100000 }
  inputs.text("firstNames") { label = "First names"; description = "One first name per line"; value = "John\nJane\nJack\nAlice\nBob"}
  inputs.text("lastNames") { label = "Last names"; description = "One last name per line"; value = "Doe\nSmith\nBrown\nJohnson\nWhite" }
}

boolean canRun() {
  return conditions.always()
}

void doRun() {
  out.info "Users XLS report generation started"

  int count = inputs.value("count")
  def firstNames = (inputs.value("firstNames")).readLines().findAll { it.trim() }
  def lastNames  = (inputs.value("lastNames")).readLines().findAll { it.trim() }

  def random = new Random()
  def now = LocalDate.now()
  def hundredYearsAgo = now.minusYears(100)

  Workbook workbook = new XSSFWorkbook()
  Sheet sheet = workbook.createSheet("Users")

  def headers = ["Name", "Surname", "Birth Date"]
  Row headerRow = sheet.createRow(0)
  headers.eachWithIndex { h, i -> headerRow.createCell(i).setCellValue(h) }

  CreationHelper helper = workbook.getCreationHelper()
  CellStyle dateStyle = workbook.createCellStyle()
  dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"))

  (1..count).each { i ->
    context.checkAborted()

    def name = firstNames[random.nextInt(firstNames.size())]
    def surname = lastNames[random.nextInt(lastNames.size())]
    def birthDate = randomDateBetween(hundredYearsAgo, now)

    Row row = sheet.createRow(i)
    row.createCell(0).setCellValue(name)
    row.createCell(1).setCellValue(surname)
    Cell dateCell = row.createCell(2)
    dateCell.setCellValue(Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    dateCell.setCellStyle(dateStyle)

    if (i % 100 == 0) out.info("Generated ${i} users...")
  }

  (0..<headers.size()).each { sheet.autoSizeColumn(it) }

  out.info "Writing report to output file..."
  def report = outputs.file("report") {
    label = "Report"
    description = "Users report generated as XLSX file"
    downloadName = "report.xlsx"
  }

  workbook.write(report.getOutputStream())
  workbook.close()

  outputs.text("summary") {
    value = "Processed ${count} user(s)"
  }

  outputs.text("docs") {
    label = "Documentation"
    description = "Get more information using following links"
    value = links([
      "Apache POI": "https://poi.apache.org/",
      "AEM Content Manager": "https://github.com/wttech/acm",
      "Adobe Experience Manager": "https://www.adobe.com/marketing/experience-manager.html"
    ])
  }

  out.success "Users XLS report generation ended successfully"
}

LocalDate randomDateBetween(LocalDate start, LocalDate end) {
  long startDay = start.toEpochDay()
  long endDay = end.toEpochDay()
  long randomDay = startDay + new Random().nextInt((int)(endDay - startDay + 1))
  return LocalDate.ofEpochDay(randomDay)
}
