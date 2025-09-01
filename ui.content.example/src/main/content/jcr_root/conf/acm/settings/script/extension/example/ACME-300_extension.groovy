import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution
import dev.vml.es.acm.core.notification.slack.SlackFactory
import dev.vml.es.acm.core.notification.slack.SlackPayload
import dev.vml.es.acm.core.notification.teams.TeamsFactory
import dev.vml.es.acm.core.notification.teams.TeamsPayload
import java.text.SimpleDateFormat

void prepareRun(ExecutionContext context) {
    context.variable("acme", new AcmeFacade())
}

class AcmeFacade {
    def now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    }
}

void completeRun(Execution execution) {
    if (conditions.isAutomaticScript()) {
        sendNotifications(execution)
    }
}

void sendNotifications(Execution execution) {
    try {
        def timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        def status = execution.status.name()
        def scriptId = execution.executable.id
        def statusIcon = status == 'SUCCESS' ? '✅' : (status == 'FAILED' ? '❌' : '⚠️')
        def environment = System.getProperty('sling.run.modes', 'unknown') // TODO fixme
    
        def slack = osgi.getService(SlackFactory.class).findDefault().orElse(null)
        if (slack && slack.enabled) {
            def slackPayload = SlackPayload.builder()
                .text("ACM Script Execution ${status}")
                .header("${statusIcon} ACM Automatic Script Execution")
                .sectionMarkdown("*Script:* `${scriptId}`")
                .divider()
                .fieldsMarkdown(
                    "*Status:* ${status}",
                    "*Execution Time:* ${timestamp}",
                    "*Duration:* ${execution.duration ?: 'N/A'}ms",
                    "*Environment:* ${environment}"
                )
                .build()
            slack.sendPayload(slackPayload)
            log.info "Sent Slack notification for script '${scriptId}' with status ${status}"
        }
        def teams = osgi.getService(TeamsFactory.class).findDefault().orElse(null)
        if (teams && teams.enabled) {
            def teamsPayload = TeamsPayload.builder()
                .title("${statusIcon} ACM Automatic Script Execution")
                .textBlock("Script **${scriptId}** executed automatically")
                .facts(
                    "Status", status,
                    "Execution Time", timestamp,
                    "Duration", "${execution.duration ?: 'N/A'}ms",
                    "Environment", environment
                )
                .build()
            teams.sendPayload(teamsPayload)
            log.info "Sent Teams notification for script '${scriptId}' with status ${status}"
        }
    } catch (Exception e) {
        log.error "Cannot send notifications for script '${scriptId}': ${e.message}", e
    }
}