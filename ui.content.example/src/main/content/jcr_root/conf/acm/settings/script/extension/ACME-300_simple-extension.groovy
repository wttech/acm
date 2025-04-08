import com.wttech.aem.acm.core.code.*

void extendRun(Shell shell) {
    shell.bindings.variable("extensionName", "Simple")
}

void completeRun(Execution execution) {
    println "Hello from the extension '${extensionName}'!"
    log.info "Completed execution: ${execution}"
}