boolean canRun() {
    return condition.idleSelf() && condition.retryChanged()
}

void doRun() {
    println("I should run only once, but will be repeated if not successful or if the content has changed!")
}
