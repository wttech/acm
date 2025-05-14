import com.vml.es.aem.acm.core.code.ExecutionContext
import com.vml.es.aem.acm.core.code.Execution
import com.vml.es.aem.acm.core.mock.MockContext

void prepareRun(ExecutionContext context) {
    context.variable("acme", new AcmeFacade())
}

void completeRun(Execution execution) {
    if (execution.status.name() == 'FAILED') {
        log.error "Something nasty happened with '${execution.executable.id}'!"
        // TODO send notification on Slack, MS Teams, etc using HTTP client / WebAPI
    }
}

void prepareMock(MockContext context) {
    context.variable("acme", new AcmeFacade())
}

class AcmeFacade {
    def now() {
        return new Date()
    }
}