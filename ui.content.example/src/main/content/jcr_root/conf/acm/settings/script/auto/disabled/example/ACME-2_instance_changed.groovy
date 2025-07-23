boolean canRun() {
    return conditions.idleSelf() && (conditions.contentChanged() || conditions.retryIfInstanceChanged())
}

void doRun() {
    throw new RuntimeException("I should run when content changes or when the instance changes after a failure!")
}
