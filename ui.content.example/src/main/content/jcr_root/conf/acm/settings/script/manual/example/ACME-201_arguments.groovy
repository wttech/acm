/**
 * Prints animal information to the console based on user input.
 *
 * This is an example of AEM Content Manager script with arguments.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */

void describeRun() {
    arguments.string("animalName") { value = "Whiskers";
        validator = "(v, a) => a.animalType === 'cat' ? (v && v.startsWith('W') || 'Cat name must start with W!') : true" }
    arguments.select("animalType") { value = "cat";
        options = ["cat", "dog", "bird", "fish", "hamster", "rabbit", "turtle", "lizard", "snake", "frog"] }
    arguments.string("secretCode") { label = "Secret Code"; value = "1234"; password() }
    arguments.bool("allergicToDogs") { label = "Allergic to Dogs?"; value = false; checkbox() }
    arguments.integerNumber("napTime") { min = 1; value = 5; group = "Behavior" }
    arguments.select("activity") { label = "Activity"; options = ["Sleeping": "sleep", "Playing": "play", "Eating": "eat"];
        value = "play"; group = "Behavior" }
    arguments.decimalNumber("hungerLevel") { min = 0.1d; max = 1.0d; value = 0.5d; group = "Behavior" }
    arguments.text("favoriteFoods") { label = "Favorite Foods"; language = "json"; value = """["milk", "mice"]""";
        group = "Data" }
    arguments.date("birthDate") { label = "Birth Date"; value = "2023-01-01"; group = "Details" }
    arguments.time("feedingTime") { label = "Feeding Time"; value = "12:00"; group = "Details" }
    arguments.dateTime("lastVetVisit") { label = "Last Vet Visit"; value = "2025-05-01T10:10:10"; group = "Details" }
    arguments.path("profilePicture") { label = "Profile Picture"; group = "Media"; rootPathExclusive = "/content/dam" }
    arguments.multiFile("holidayPictures") { label = "Holiday Pictures"; group = "Media"; images(); optional(); max = 3 }
    arguments.color("favoriteColor") { label = "Favorite Color"; value = "#ffcc00"; group = "Media" }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println "Animal Name: ${arguments.value('animalName')}"
    println "Animal Type: ${arguments.value('animalType')}"
    println "Activity: ${arguments.value('activity')}"
    println "Allergic to Dogs: ${arguments.value('allergicToDogs')}"
    println "Birth Date: ${arguments.value('birthDate')}"
    println "Feeding Time: ${arguments.value('feedingTime')}"
    println "Last Vet Visit: ${arguments.value('lastVetVisit')}"
    println "Favorite Color: ${arguments.value('favoriteColor')}"
    println "Secret Code: ${arguments.value('secretCode')}"
    println "Profile Picture: ${arguments.value('profilePicture')}"

    if (arguments.value('allergicToDogs') && arguments.value('animalType') == 'dog') {
        println "${arguments.value('animalName')} cannot be around dogs due to allergies!"
    } else {
        println "${arguments.value('animalName')} the ${arguments.value('animalType')} is ready for some fun!"
    }

    switch (arguments.value('activity')) {
        case 'sleep':
            println "${arguments.value('animalName')} is taking a nap for ${arguments.value('napTime')} minutes... Zzz..."
            Thread.sleep(arguments.value('napTime') * 1000)
            println "${arguments.value('animalName')} woke up refreshed!"
            break
        case 'play':
            println "${arguments.value('animalName')} is playing with a ball of yarn!"
            for (int i = 0; i < 5; i++) {
                println "Playing... ${i + 1}"
                Thread.sleep(1000)
            }
            println "${arguments.value('animalName')} is tired now."
            break
        case 'eat':
            println "${arguments.value('animalName')} is eating their favorite foods."
            def foods = formatter.json.readFromString(arguments.value('favoriteFoods'), String[].class)
            foods.each { food ->
                println "Eating ${food}..."
                Thread.sleep(1000)
            }
            println "${arguments.value('animalName')} is full now."
            break
        default:
            println "${arguments.value('animalName')} is just chilling."
    }

    println "Hunger Level: ${arguments.value('hungerLevel') * 100}%"
}