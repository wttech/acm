import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution

/*
Provides binding variable 'acme' and logs error on execution failure.
*/

void prepareRun(ExecutionContext context) {
    context.variable("acme", new AcmeFacade())
}

void completeRun(Execution execution) {
    if (execution.status.name() == 'FAILED') {
        log.error "Something nasty happened with '${execution.executable.id}'!"
        // TODO send notification using built-in 'notifier' for Slack and MS Teams or use custom HTTP client / WebAPI
    }
}

class AcmeFacade {
    def now() {
        return new Date()
    }
}