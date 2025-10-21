import dev.vml.es.acm.core.code.ExecutionContext
import dev.vml.es.acm.core.code.Execution
import java.util.Calendar

void prepareRun(ExecutionContext context) {
    def minute = Calendar.getInstance().get(Calendar.MINUTE)
    if (minute % 2 == 0) {
        context.skipped = true
    }
}

void completeRun(Execution execution) {
    log.info "I am not skipped on even minutes!"
}