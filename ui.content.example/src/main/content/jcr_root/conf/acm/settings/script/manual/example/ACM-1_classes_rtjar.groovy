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
import com.vml.es.aem.acm.core.assist.JavaClassDictionary
import java.util.function.Consumer
import java.util.jar.JarFile
import java.io.File

void describeRun() {
    args.select("mode") { options = ["print", "save"]; value = "print" }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    out.fromLogs()

    switch (args.value("mode")) {
        case "print":
            eachSystemClass { className -> println "${className}"}
            break;
        case "save":
            def buffer = new StringBuffer();
            eachSystemClass { className -> buffer.append("${className}\n")}
            def dictFile = repo.get(JavaClassDictionary.path())
            dictFile.parent().ensureFolder()
            dictFile.saveFile(buffer.toString(), "text/plain")
            break;
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