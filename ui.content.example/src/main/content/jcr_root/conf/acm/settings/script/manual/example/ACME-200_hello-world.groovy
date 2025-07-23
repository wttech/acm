/**
 * Prints "Hello World!" to the console.
 *
 * This is a minimal example of AEM Content Manager script.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */

boolean canRun() {
    return conditions.always()
}

void doRun() {
    println "Hello World!"
}