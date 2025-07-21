boolean canRun() {
    return condition.idleSelf() && (condition.contentChanged() || condition.retryIfInstanceChanged())
}

void doRun() {
    throw new RuntimeException("I should run when content changes or when the instance changes after a failure!")
}
