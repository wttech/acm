/**
 * Generate a list of all system classes in the current JVM.
 * This version of the script is designed to be run in the context of AEM running on Java 8.
 *
 * Arguments allow to:
 * - print the list (for debugging purposes),
 * - save it directly in the repository in expected path.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */

import dev.vml.es.acm.core.assist.JavaDictionary
import java.util.function.Consumer
import java.util.jar.JarFile
import java.io.File

void describeRun() {
    arguments.select("mode") { options = ["print", "save"]; value = "print" }
}

boolean canRun() {
    return conditions.always()
}

void doRun() {
    out.fromLogs()

    switch (arguments.value("mode")) {
        case "print":
            eachSystemClass { className -> println "${className}" }
            break
        case "save":
            def modules = [(JavaDictionary.RTJAR_MODULE): []]
            eachSystemClass { className -> modules[JavaDictionary.RTJAR_MODULE] << className }
            JavaDictionary.save(resourceResolver, modules)
            break
    }
}

def eachSystemClass(Consumer<String> callback) {
    osgi.osgiScanner.getSystemExportedPackages().sorted().forEach { pkg ->
        findSystemClasses(pkg).sorted().forEach { className ->
            callback(className)
        }
    }
}

def findSystemClasses(String packageName) {
    def javaHome = System.getProperty("java.home")
    def javaLibPath = new File(javaHome, "lib").canonicalPath

    def classPathEntries = [] as List<String>
    classPathEntries.add(javaLibPath + File.separator + "rt.jar")
    classPathEntries.addAll(System.getProperty("java.class.path").split(System.getProperty("path.separator")))

    return classPathEntries.stream()
            .filter { entry -> entry.endsWith(".jar") }
            .flatMap { entry -> findClassesInJar(new File(entry), packageName) }
}

def findClassesInJar(File jarFile, String packageName) {
    def path = packageName.replace('.', '/')
    def jar = new JarFile(jarFile)
    return jar.stream()
            .filter { entry -> entry.name.startsWith(path) && entry.name.endsWith(".class") }
            .map { entry -> osgi.osgiScanner.normalizeClassName(entry.name).orElse(null) }
            .filter { Objects.nonNull(it)}
}