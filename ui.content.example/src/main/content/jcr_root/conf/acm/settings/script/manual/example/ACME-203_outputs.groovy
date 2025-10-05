boolean canRun() {
    return conditions.always()
}

void doRun() { 
    log.info "Users report generation started"

    def report = outputs.file("report") {
        label = "Report"
        description = "Users report generated as CSV file"
        downloadName = "report.csv"
    }

    def users = [
        [name: "John", surname: "Doe", birth: "1991"],
        [name: "Jane", surname: "Doe", birth: "1995"],
        [name: "Jack", surname: "Doe", birth: "2000"]
    ] 
    for (def user : users) {
        report.out.println("${user.name},${user.surname},${user.birth}")
    }

    outputs.text("summary") {
        label = "Summary"
        description = "Total users count and links"
        links = [
            "Google": "https://www.google.com",
            "AEM": "https://www.adobe.com/marketing/experience-manager.html"
        ]
    }

    log.info "Users report generation ended successfully"
}