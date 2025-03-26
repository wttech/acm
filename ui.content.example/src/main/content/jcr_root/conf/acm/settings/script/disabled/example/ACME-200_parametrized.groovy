void describeRun() {
    //args.checkbox("replicate")
    args.bool("dryRun") { label = "Dry run"; value = true; checkbox() }
    args.string("name") { value = "John" }
    args.string("surname") { value = "Doe" }
    args.select("mode") { label = "Mode"; options = ["label 1": "v1", "label 2": "v2"]; value = "v2" }
    args.text("mappings") { label = "Mappings"; language = "json"; value = "{}"; group = "Data" }
    args.text("userData") { label = "User data"; language = "csv"; group = "Data"}
    args.integerNumber("batchSize") { min = 1; max = 30; value = 10 }
    args.doubleNumber("probability") { min = 0.1d; max = 1.0d; value = 0.5d }
    // TODO protect: args.doubleNumber("probability") { min = 0.1; max = 1.0; value = 0.5 }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    println "Hello ${args.value('name')} ${args.value('surname')}!"
}