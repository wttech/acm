void describeRun() {
    args.bool("isCat") { label = "Is it a cat?"; value = true; checkbox() }
    args.string("animalName") { value = "Whiskers" }
    args.string("animalType") { value = "Cat" }
    args.select("activity") { label = "Activity"; options = ["Sleeping": "sleep", "Playing": "play", "Eating": "eat"]; value = "play" }
    args.text("favoriteFoods") { label = "Favorite Foods"; language = "json"; value = """["milk", "mice"]"""; group = "Data" }
    args.integerNumber("napTime") { min = 1; value = 5 }
    args.decimalNumber("hungerLevel") { min = 0.1d; max = 1.0d; value = 0.5d }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    println "Is it a cat? ${args.value('isCat')}"
    println "Animal Name: ${args.value('animalName')}"
    println "Animal Type: ${args.value('animalType')}"
    println "Activity: ${args.value('activity')}"

    if (args.value('isCat')) {
        println "${args.value('animalName')} the cat is ready for some fun!"
    } else {
        println "${args.value('animalName')} the ${args.value('animalType')} is ready for some fun!"
    }

    switch (args.value('activity')) {
        case 'sleep':
            println "${args.value('animalName')} is taking a nap for ${args.value('napTime')} minutes... Zzz..."
            Thread.sleep(args.value('napTime') * 1000)
            println "${args.value('animalName')} woke up refreshed!"
            break
        case 'play':
            println "${args.value('animalName')} is playing with a ball of yarn!"
            for (int i = 0; i < 5; i++) {
                println "Playing... ${i + 1}"
                Thread.sleep(1000)
            }
            println "${args.value('animalName')} is tired now."
            break
        case 'eat':
            println "${args.value('animalName')} is eating their favorite foods."
            def foods = formatter.json.readFromString(args.value('favoriteFoods'), String[].class)
            foods.each { food ->
                println "Eating ${food}..."
                Thread.sleep(1000)
            }
            println "${args.value('animalName')} is full now."
            break
        default:
            println "${args.value('animalName')} is just chilling."
    }

    println "Hunger Level: ${args.value('hungerLevel') * 100}%"
}