/**
 * Generate a list of all system classes in the current JVM.
 * This version of the script is designed to be run in the context of AEM running on Java 9+.
 *
 * Arguments allow to:
 * - print the list (for debugging purposes),
 * - save it directly in the repository in expected path.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */
import com.vml.es.aem.acm.core.assist.JavaClassDictionary

void describeRun() {
    args.select("mode") { options = ["print", "save"]; value = "print" }
}

boolean canRun() {
    return condition.always()
}

void doRun() {
    out.fromLogs()

    def buffer = new StringBuffer()
    def result = eachSystemClass()
    mapToYaml(result, buffer)
    switch (args.value("mode")) {
        case "print":
            println(buffer.toString())
            break
        case "save":
            def dictFile = repo.get(JavaClassDictionary.path())
            dictFile.parent().ensureFolder()
            dictFile.saveFile(buffer.toString(), "application/x-yaml")
            break
    }
}

def eachSystemClass() {
    def exportedPackages = osgi.osgiScanner.getSystemExportedPackages().sorted().toList()
    def classMap = [:]
    ModuleLayer.boot().modules().forEach { module ->
        module.getPackages().findAll { pkg -> exportedPackages.contains(pkg) }.forEach { pkg ->
            findSystemClasses(module, pkg).forEach { m ->
                if (classMap.containsKey(m[1])) {
                    classMap[m[1]].add(m[0])
                } else {
                    classMap[m[1]] = [m[0]]
                }
            }
        }
    }
    return classMap
}

def findSystemClasses(Module module, String packageName) {
    def classNames = [] as List<List<String>>
    def packagePathPrefix = packageName.replace('.', '/')

    module.getLayer().configuration().modules().stream()
            .filter { it.name() == module.name }
            .findFirst()
            .ifPresent { resolvedModule ->
                resolvedModule.reference().open().withCloseable { moduleReader ->
                    moduleReader.list()
                            .filter { resource -> resource.startsWith(packagePathPrefix) && resource.endsWith(".class") }
                            .forEach { resource ->
                                def className = resource.replace('/', '.').replace('.class', '')
                                def normalizedClassName = osgi.osgiScanner.normalizeClassName(className).orElse(null)
                                if (normalizedClassName) {
                                    classNames.add([normalizedClassName, resolvedModule.name()])
                                }
                            }
                }
            }

    return classNames.stream()
}

def mapToYaml(Map<String, List<String>> map, StringBuffer stringBuffer) {
    map.each { key, valueList ->
        stringBuffer.append("${key}:\n")
        valueList.each { value ->
            stringBuffer.append("   - ${value}\n")
        }
    }
}