boolean canRun() {
    return condition.idleSelf() && condition.once()
}

void doRun() {
    println("I will fail soon...")
    throw new RuntimeException("I am failing!")
}
