void extendRun() {
    extender.variable("myVar") { "myValue" }
}

void completeRun() {
    log.info "Completed execution: ${extender.execution}"
}