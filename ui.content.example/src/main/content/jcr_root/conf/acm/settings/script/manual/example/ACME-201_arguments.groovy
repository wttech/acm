/**
 * Prints animal information to the console based on user input.
 *
 * This is an example of AEM Content Manager script with arguments.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */
void describeRun() {
    args.string("animalName") { value = "Whiskers" ; validator = "(v, a) => a.animalType === 'cat' ? (v && v.startsWith('W') || 'Cat name must start with W!') : true" }
    args.select("animalType") { value = "cat"; options = ["cat", "dog", "bird", "fish", "hamster", "rabbit", "turtle", "lizard", "snake", "frog"] }
    args.bool("birthday") { label = "Birthday?"; value = false; checkbox() }
    args.integerNumber("napTime") { min = 1; value = 5; group = "Behavior" }
    args.select("activity") { label = "Activity"; options = ["Sleeping": "sleep", "Playing": "play", "Eating": "eat"]; value = "play"; group = "Behavior" }
    args.decimalNumber("hungerLevel") { min = 0.1d; max = 1.0d; value = 0.5d; group = "Behavior" }
    args.text("favoriteFoods") { label = "Favorite Foods"; language = "json"; value = """["milk", "mice"]"""; group = "Data" }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    println "Animal Name: ${args.value('animalName')}"
    println "Animal Type: ${args.value('animalType')}"
    println "Activity: ${args.value('activity')}"
    println "Birthday: ${args.value('birthday')}"

    if (args.value('birthday')) {
        println "Happy Birthday, ${args.value('animalName')} the ${args.value('animalType')}!"
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