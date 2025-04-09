import com.wttech.aem.acm.core.code.script.ContentScript
import com.wttech.aem.acm.core.code.Execution

void extendRun(ContentScript script) {
    script.variable("extensionName", "Simple")
}

void completeRun(Execution execution) {
    println "Hello from the extension '${extensionName}'!"
    log.info "Completed execution: ${execution}"
}