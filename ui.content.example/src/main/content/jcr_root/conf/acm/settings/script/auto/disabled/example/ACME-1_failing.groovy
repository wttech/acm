boolean canRun() {
    return condition.retry(3)
}

void doRun() {
    println("I will fail soon...")
    Thread.sleep(1000)
    throw new RuntimeException("I am failing!")
}
