import java.time.LocalDate
import java.util.Random

boolean canRun() {
  return conditions.always()
}

void describeRun() {
  inputs.integerNumber("count") { label = "Users to generate"; min = 1; value = 10000 }
  inputs.text("firstNames") { label = "First names"; description = "One first name per line"; value = "John\nJane\nJack\nAlice\nBob"}
  inputs.text("lastNames") { label = "Last names"; description = "One last name per line"; value = "Doe\nSmith\nBrown\nJohnson\nWhite" }
}

void doRun() {
  out.info "Users CSV report generation started"

  int count = inputs.value("count")
  def firstNames = (inputs.value("firstNames")).readLines().findAll { it.trim() }
  def lastNames  = (inputs.value("lastNames")).readLines().findAll { it.trim() }

  def random = new Random()
  def now = LocalDate.now()
  def hundredYearsAgo = now.minusYears(100)

  def report = outputs.file("report") {
    label = "Report"
    description = "Users report generated as CSV file"
    downloadName = "report.csv"
  }

  // CSV header
  report.out.println("Name,Surname,Birth Date")

  // Generate users
  (1..count).each { i ->
    context.checkAborted()
    
    def name = firstNames[random.nextInt(firstNames.size())]
    def surname = lastNames[random.nextInt(lastNames.size())]
    def birthDate = randomDateBetween(hundredYearsAgo, now)

    report.out.println("${name},${surname},${birthDate}")

    if (i % 100 == 0) out.info("Generated ${i} users...")
  }

  outputs.text("summary") {
    value = "Processed ${count} user(s)"
  }

  out.success "Users CSV report generation ended successfully"
}

LocalDate randomDateBetween(LocalDate start, LocalDate end) {
  long startDay = start.toEpochDay()
  long endDay = end.toEpochDay()
  long randomDay = startDay + new Random().nextInt((int)(endDay - startDay + 1))
  return LocalDate.ofEpochDay(randomDay)
}
