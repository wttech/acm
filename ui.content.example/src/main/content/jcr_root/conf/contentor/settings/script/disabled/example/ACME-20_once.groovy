boolean canRun() {
    return condition.once()
}

void doRun() {
    println("I should run only once!")
}
