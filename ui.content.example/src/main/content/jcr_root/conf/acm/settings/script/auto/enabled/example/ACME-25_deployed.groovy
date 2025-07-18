boolean canRun() {
    return condition.deployed() && condition.idleSelf() && condition.retryChanged()
}

void doRun() {
    println("I should run only once per deployment and be repeated on consecutive deployments when unsuccessful or when the content has changed!")
}
