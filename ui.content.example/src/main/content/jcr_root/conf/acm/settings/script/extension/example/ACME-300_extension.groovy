import com.vml.es.aem.acm.core.code.ExecutionContext
import com.vml.es.aem.acm.core.code.Execution

void prepareRun(ExecutionContext executionContext) {
    executionContext.variable("acme", new AcmeFacade())
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