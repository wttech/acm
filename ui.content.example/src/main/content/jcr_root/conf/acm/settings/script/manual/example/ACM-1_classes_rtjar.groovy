import com.vml.es.aem.acm.core.osgi.OsgiScanner
import java.util.jar.JarFile


boolean describeRun() {
    args.select("mode") { options = ["print", "save"] }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    def osgiScanner = osgi.getService(OsgiScanner.class)
    def exportedPackages = osgiScanner.findSystemExportedPackages().sort()
    switch (args.value("mode")) {
        case "print":
            exportedPackages.each { packageName ->
                findSystemClasses(osgiScanner, packageName).each { className -> println "${className}"}
            }
            break;
        case "save":
            def buffer = new StringBuffer();
            exportedPackages.sort().each { packageName ->
                findSystemClasses(osgiScanner, packageName).each { className -> buffer.append("${className}\n")}
            }
            repo.saveFile("/conf/acm/settings/assist/jre/1.8.txt", buffer.toString(), "text/plain")
            break;
    }
}

List<String> findSystemClasses(OsgiScanner osgiScanner, String packageName) {
    def javaHome = System.getProperty("java.home")
    def javaLibPath = new File(javaHome, "lib").canonicalPath

    def classPath = [] as List<String>
    classPath.add(javaLibPath + System.getProperty("path.separator") + "rt.jar")
    classPath.addAll(System.getProperty("java.class.path").split(System.getProperty("path.separator")))

    def classNames = []
    classPath.each { entry ->
        def file = new File(entry)
        if (file.name.endsWith(".jar")) {
            classNames.addAll(findClassesInJar(osgiScanner, file, packageName))
        }
    }
    return classNames
}

List<String> findClassesInJar(OsgiScanner osgiScanner, File jarFile, String packageName) {
    def classNames = [] as List<String>
    def path = packageName.replace('.', '/')
    def jar = new JarFile(jarFile)
    jar.entries().each { entry ->
        if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
            classNames.add(osgiScanner.normalizeClassName(entry.name))
        }
    }
    return classNames
}