boolean canRun() {
    return condition.idle() && condition.once()
}

void doRun() {
    println("I should run only once!")
}
