import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution

import a.b.c.d.NonExistingClass

void prepareRun(ExecutionContext context) {
    context.variable("acme", new AcmeFacade())
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