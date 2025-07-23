boolean canRun() {
    return conditions.idleSelf() && conditions.once()
}

void doRun() {
    println("I should run only once!")
}
