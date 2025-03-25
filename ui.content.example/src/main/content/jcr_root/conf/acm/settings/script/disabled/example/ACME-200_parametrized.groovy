void describeRun() {
    //args.checkbox("replicate")
    args.toggle("dryRun") { label = "Dry run" }
    args.string("name") { value = "John" }
    args.string("surname") { value = "Doe" }
    args.select("mode") { label = "Mode"; options = ["k1": "v1", "k2": "v2"]; value = "v1" }
    args.text("userData") { label = "User data"; language = "csv"}
    args.text("mappings") { label = "Mappings"; language = "json"; value = "{}" }
    args.integerNumber("batchSize") { min = 1; max = 30; value = 10 }
    args.doubleNumber("probability") { min = 0.1d; max = 1.0d; value = 0.5d }
    // TODO protect: args.doubleNumber("probability") { min = 0.1; max = 1.0; value = 0.5 }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    println "Hello World!"
    // println "Hello ${args.value("name")} ${args.value("surname")}!"
}