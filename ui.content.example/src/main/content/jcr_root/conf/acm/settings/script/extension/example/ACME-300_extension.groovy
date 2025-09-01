import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution

void prepareRun(ExecutionContext context) {
    context.variable("acme", new AcmeFacade())
}

void completeRun(Execution execution) {
    if (execution.status.name() == 'FAILED') {
        log.error "Something nasty happened with '${execution.executable.id}'!"
        // TODO send notification on Slack, MS Teams, etc using HTTP client / WebAPI

        /*
        // Basic notification
        SlackPayload basicPayload = SlackPayload.builder()
            .text("Server maintenance completed successfully")
            .header("System Alert")
            .sectionPlain("All systems are operational")
            .build();

        // Rich notification with blocks
        SlackPayload richPayload = SlackPayload.builder()
            .text("Critical Alert") // Fallback text for notifications
            .header("🚨 Critical Database Alert")
            .sectionMarkdown("*Database connection failure* detected on production server")
            .divider()
            .fieldsMarkdown(
                "*Status:* Critical",
                "*Server:* db-prod-01",
                "*Time:* 2024-09-01 10:54:00", 
                "*Affected Users:* 1,247"
            )
            .divider()
            .sectionMarkdown("*Next Steps:* Check database logs and restart if necessary")
            .build();

        // Basic notification
        TeamsPayload basicPayload = TeamsPayload.builder()
            .title("System Alert")
            .text("Server maintenance completed successfully")
            .build();

        // Rich notification with facts and actions
        TeamsPayload richPayload = TeamsPayload.builder()
            .title("🚨 Critical Alert")
            .textBlock("Database connection failure detected on production server")
            .facts(
                "Status", "Critical",
                "Server", "db-prod-01", 
                "Time", "2024-09-01 10:54:00",
                "Affected Users", "1,247"
            )
            .textBlock("**Next Steps:** Check database logs and restart if necessary")
            .openUrlAction("View Dashboard", "https://monitoring.example.com/db-prod-01")
            .openUrlAction("View Logs", "https://logs.example.com/db-prod-01")
            .build();
        */
    }
}

class AcmeFacade {
    def now() {
        return new Date()
    }
}