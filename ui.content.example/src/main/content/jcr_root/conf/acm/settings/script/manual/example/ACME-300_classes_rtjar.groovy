import org.osgi.framework.*
import org.osgi.framework.wiring.BundleWiring
import java.util.jar.JarFile

boolean canRun() {
    return condition.always()
}

void doRun() {
    def exportedPackages = getSystemBundlePackages()
    exportedPackages.sort().each { packageName ->
        def classes = findSystemClasses(packageName)
        classes.each { className -> println "${className}"}
    }
}

List<String> getSystemBundlePackages() {
    def bundle = osgi.bundleContext.getBundle(0)
    def bundleWiring = bundle.adapt(BundleWiring.class)
    return bundleWiring?.getCapabilities("osgi.wiring.package")?.collect { c -> c.attributes["osgi.wiring.package"]} ?: []
}

List<String> findSystemClasses(String packageName) {
    def classNames = []
    def javaHome = System.getProperty("java.home")
    def javaLibPath = new File(javaHome, "lib").canonicalPath

    def classpathEntries = []
    classpathEntries.add(javaLibPath + "/rt.jar")
    classpathEntries.addAll(System.getProperty("java.class.path").split(System.getProperty("path.separator")))

    classpathEntries.each { entry ->
        def file = new File(entry)
        if (file.name.endsWith(".jar")) {
            classNames.addAll(findClassesInJar(file, packageName))
        }
    }
    return classNames
}

List<String> findClassesInJar(File jarFile, String packageName) {
    def classNames = []
    def path = packageName.replace('.', '/')
    def jar = new JarFile(jarFile)
    jar.entries().each { entry ->
        if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
            // TODO normalize like in OSGi scanner
            def className = entry.name.replace("/", ".").replace(".class", "")
            classNames << className
        }
    }
    return classNames
}