boolean canRun() {
    return condition.idleSelf() && condition.once()
}

void doRun() {
    println("I should run only once!")
}
