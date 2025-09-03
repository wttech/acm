import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution
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
        log.debug "Skipped notifications for script '${execution.executable.id}' - notifier not configured!"
        return
    }
    try {
        def timestamp = acme.now()
        def status = execution.status.name()
        def scriptId = execution.executable.id
        def statusIcon = status == 'SUCCESS' ? '✅' : (status == 'FAILED' ? '❌' : '⚠️')
        def instanceSettings = conditions.instanceInfo().settings
    
        def title = "${statusIcon} ACM Automatic Script Execution"
        def text = "Script '${scriptId}' executed automatically with status '${status}'"
        def fields = [
            "Status": status,
            "Execution Time": timestamp,
            "Duration": "${execution.duration ?: 'N/A'}ms".toString(),
            "Instance": "${instanceSettings.id} (${instanceSettings.role})".toString(),
        ]
        
        notifier.sendMessage(title, text, fields)
        log.info "Sent notification for script '${scriptId}' with status ${status}"
    } catch (Exception e) {
        log.error "Cannot send notifications for script '${execution.executable.id}': ${e.message}", e
    }
}