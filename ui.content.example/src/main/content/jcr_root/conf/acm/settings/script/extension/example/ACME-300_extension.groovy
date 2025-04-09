import com.wttech.aem.acm.core.code.script.ContentScript
import com.wttech.aem.acm.core.code.Execution

void extendRun(ContentScript script) {
    script.variable("acme", new AcmeFacade())
}

void completeRun(Execution execution) {
    log.info "Completed execution: ${execution}"
}

class AcmeFacade {
    def now() {
        return new Date()
    }
}