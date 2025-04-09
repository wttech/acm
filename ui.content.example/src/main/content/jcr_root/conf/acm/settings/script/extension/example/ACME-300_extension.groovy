import com.wttech.aem.acm.core.code.script.ContentScript
import com.wttech.aem.acm.core.code.Execution

void extendRun(ContentScript script) {
    script.variable("acme", new AcmeFacade())
}

void completeRun(Execution execution) {
    if (execution.status.name() == 'FAILED') {
        log.error "Something nasty happened with '${execution.executable.id}'!"
        // TODO send notification on Slack, MS Teams, etc using HTTP client / WebAPI
    }
}

class AcmeFacade {
    def now() {
        return new Date()
    }
}