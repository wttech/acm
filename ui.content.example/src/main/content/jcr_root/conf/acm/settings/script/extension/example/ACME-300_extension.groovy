import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution
import dev.vml.es.acm.core.code.ExecutionStatus
import dev.vml.es.acm.core.notification.NotifierManager
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
    if (!notifier.isConfigured()) {
        log.debug "Skipped notifications for script '${execution.executable.id}' (not configured)"
        return
    }
    try {
        def timestamp = acme.now()
        def status = execution.status
        def scriptId = execution.executable.id
        def statusName = status.name().toLowerCase()
        def statusIcon = status == ExecutionStatus.SUCCEEDED ? '✅' : (status == ExecutionStatus.FAILED ? '❌' : '⚠️')
        def instanceSettings = conditions.instanceInfo().settings
        def instanceRoleName = instanceSettings.role.name().toLowerCase()
    
        def title = "${statusIcon} ACM Automatic Script Execution"
        def text = "Script '${scriptId}' completed with status '${statusName}'"
        def fields = [
            "Status": statusName,
            "Execution Time": timestamp,
            "Duration": "${execution.duration}ms",
            "Instance": "${instanceSettings.id} (${instanceRoleName})",
        ]
        
        notifier.sendMessage(title, text, fields)
        log.info "Sent notification for script '${scriptId}' with status ${statusName}"
    } catch (Exception e) {
        log.error "Cannot send notifications for script '${execution.executable.id}': ${e.message}", e
    }
}