import com.vml.es.aem.acm.core.assist.JavaClassDictionary
import com.vml.es.aem.acm.core.osgi.OsgiScanner

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
            dictFile.parent().makeFolders()
            dictFile.saveFile(buffer.toString(), "text/plain")
            break
    }
}

OsgiScanner osgiScanner() { return osgi.getService(OsgiScanner.class) }

def eachSystemClass(Consumer<String> callback) {
    def exportedPackages = osgiScanner().findSystemExportedPackages().sorted().toList()
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
                                def normalizedClassName = osgiScanner().normalizeClassName(className).orElse(null)
                                if (normalizedClassName) {
                                    classNames.add(normalizedClassName)
                                }
                            }
                }
            }

    return classNames.stream()
}