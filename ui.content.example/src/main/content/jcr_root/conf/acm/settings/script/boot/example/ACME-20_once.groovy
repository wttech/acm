boolean canRun() {
    return conditions.once()
}

void doRun() {
    println("I should run only once!")
}
