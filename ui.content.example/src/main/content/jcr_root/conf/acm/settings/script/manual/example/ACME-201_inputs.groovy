/**
 * @description Prints animal information to the console based on user input. This is an example of AEM Content Manager script with inputs.
 * @author Krystian Panek <krystian.panek@vml.com>
 */
 
void describeRun() {
    inputs.string("animalName") { value = "Whiskers";
        validator = "(v, a) => a.animalType === 'cat' ? (v && v.startsWith('W') || 'Cat name must start with W!') : true" }
    inputs.select("animalType") { value = "cat";
        options = ["cat", "dog", "bird", "fish", "hamster", "rabbit", "turtle", "lizard", "snake", "frog"] }
    inputs.string("secretCode") { label = "Secret Code"; value = "1234"; password() }
    inputs.bool("allergicToDogs") { label = "Allergic to Dogs?"; value = false; checkbox() }
    inputs.integerNumber("napTime") { min = 1; value = 5; group = "Behavior" }
    inputs.select("activity") { label = "Activity"; options = ["Sleeping": "sleep", "Playing": "play", "Eating": "eat"];
        value = "play"; group = "Behavior" }
    inputs.decimalNumber("hungerLevel") { min = 0.1d; max = 1.0d; value = 0.5d; group = "Behavior" }
    inputs.text("favoriteFoods") { label = "Favorite Foods"; language = "json"; value = """["milk", "mice"]""";
        group = "Data" }
    inputs.date("birthDate") { label = "Birth Date"; value = "2023-01-01"; group = "Details" }
    inputs.time("feedingTime") { label = "Feeding Time"; value = "12:00"; group = "Details" }
    inputs.dateTime("lastVetVisit") { label = "Last Vet Visit"; value = "2025-05-01T10:10:10"; group = "Details" }
    inputs.path("profilePicture") { label = "Profile Picture"; group = "Media"; rootPathExclusive = "/content/dam" }
    inputs.multiFile("holidayPictures") { label = "Holiday Pictures"; group = "Media"; images(); optional(); max = 3 }
    inputs.color("favoriteColor") { label = "Favorite Color"; value = "#ffcc00"; group = "Media" }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println "Animal Name: ${inputs.value('animalName')}"
    println "Animal Type: ${inputs.value('animalType')}"
    println "Activity: ${inputs.value('activity')}"
    println "Allergic to Dogs: ${inputs.value('allergicToDogs')}"
    println "Birth Date: ${inputs.value('birthDate')}"
    println "Feeding Time: ${inputs.value('feedingTime')}"
    println "Last Vet Visit: ${inputs.value('lastVetVisit')}"
    println "Favorite Color: ${inputs.value('favoriteColor')}"
    println "Secret Code: ${inputs.value('secretCode')}"
    println "Profile Picture: ${inputs.value('profilePicture')}"

    if (inputs.value('allergicToDogs') && inputs.value('animalType') == 'dog') {
        println "${inputs.value('animalName')} cannot be around dogs due to allergies!"
    } else {
        println "${inputs.value('animalName')} the ${inputs.value('animalType')} is ready for some fun!"
    }

    switch (inputs.value('activity')) {
        case 'sleep':
            println "${inputs.value('animalName')} is taking a nap for ${inputs.value('napTime')} minutes... Zzz..."
            Thread.sleep(inputs.value('napTime') * 1000)
            println "${inputs.value('animalName')} woke up refreshed!"
            break
        case 'play':
            println "${inputs.value('animalName')} is playing with a ball of yarn!"
            for (int i = 0; i < 5; i++) {
                println "Playing... ${i + 1}"
                Thread.sleep(1000)
            }
            println "${inputs.value('animalName')} is tired now."
            break
        case 'eat':
            println "${inputs.value('animalName')} is eating their favorite foods."
            def foods = formatter.json.readFromString(inputs.value('favoriteFoods'), String[].class)
            foods.each { food ->
                println "Eating ${food}..."
                Thread.sleep(1000)
            }
            println "${inputs.value('animalName')} is full now."
            break
        default:
            println "${inputs.value('animalName')} is just chilling."
    }

    println "Hunger Level: ${inputs.value('hungerLevel') * 100}%"
}