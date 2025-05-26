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
import dev.vml.es.acm.core.assist.JavaClassDictionary

import java.lang.module.ModuleFinder
import java.lang.module.ModuleReference
import java.util.function.Consumer

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
            eachSystemClass { className -> println "${className}" }
            break
        case "save":
            def buffer = new StringBuffer()
            eachSystemClass { className -> buffer.append("${className}\n") }
            def dictFile = repo.get(JavaClassDictionary.path())
            dictFile.parent().ensureFolder()
            dictFile.saveFile(buffer.toString(), "text/plain")
            // TODO JavaClassDictionary.save(resourceResolver, modules)
            break
    }
}

def eachSystemClass(Consumer<String> callback) {
    def exportedPackages = osgi.osgiScanner.getSystemExportedPackages().sorted().toList()
    ModuleLayer.boot().modules().forEach { module ->
        module.getPackages().findAll { pkg -> exportedPackages.contains(pkg) }.forEach { pkg ->
            findSystemClasses(module, pkg).sorted().forEach { className ->
                callback(className)
            }
        }
    }
}

def findSystemClasses(Module module, String packageName) {
    def classNames = [] as List<String>
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
                                    classNames.add(normalizedClassName)
                                }
                            }
                }
            }

    return classNames.stream()
}