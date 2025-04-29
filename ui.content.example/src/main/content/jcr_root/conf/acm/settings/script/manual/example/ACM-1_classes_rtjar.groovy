import com.vml.es.aem.acm.core.osgi.OsgiScanner
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

    def osgiScanner = osgi.getService(OsgiScanner.class)
    switch (args.value("mode")) {
        case "print":
            eachSystemClass(osgiScanner) { className -> println "${className}"}
            break;
        case "save":
            def buffer = new StringBuffer();
            eachSystemClass(osgiScanner) { className -> buffer.append("${className}\n")}
            def dictFile = repo.get(JavaClassDictionary.path())
            dictFile.parent().makeFolders()
            dictFile.saveFile(buffer.toString(), "text/plain")
            break;
    }
}

def eachSystemClass(OsgiScanner osgiScanner, Consumer<String> callback) {
    osgiScanner.findSystemExportedPackages().forEach { pkg ->
        findSystemClasses(osgiScanner, pkg).forEach { className ->
            callback(className)
        }
    }
}

def findSystemClasses(OsgiScanner osgiScanner, String packageName) {
    def javaHome = System.getProperty("java.home")
    def javaLibPath = new File(javaHome, "lib").canonicalPath

    def classPathEntries = [] as List<String>
    classPathEntries.add(javaLibPath + File.separator + "rt.jar")
    classPathEntries.addAll(System.getProperty("java.class.path").split(System.getProperty("path.separator")))

    return classPathEntries.stream()
            .filter { entry -> entry.endsWith(".jar") }
            .flatMap { entry -> findClassesInJar(osgiScanner, new File(entry), packageName) }
}

def findClassesInJar(OsgiScanner osgiScanner, File jarFile, String packageName) {
    def path = packageName.replace('.', '/')
    def jar = new JarFile(jarFile)
    return jar.stream()
            .filter { entry -> entry.name.startsWith(path) && entry.name.endsWith(".class") }
            .map { entry -> osgiScanner.normalizeClassName(entry.name).orElse(null) }
            .filter { Objects.nonNull(it)}
}