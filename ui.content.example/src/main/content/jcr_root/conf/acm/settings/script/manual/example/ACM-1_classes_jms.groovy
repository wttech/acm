import com.vml.es.aem.acm.core.assist.JavaClassDictionary

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

def eachSystemClass(Consumer<String> callback) {
    ModuleLayer.boot().modules().forEach { module ->
        module.getPackages().forEach { pkg ->
            findSystemClasses(module, pkg).forEach { className ->
                callback(className)
            }
        }
    }
}

def findSystemClasses(Module module, String packageName) {
    def classLoader = module.getClassLoader()
    def path = packageName.replace('.', '/')
    def resources = classLoader?.getResources(path) ?: []
    def classNames = [] as List<String>

    resources.each { url ->
        def file = new File(url.toURI())
        if (file.isDirectory()) {
            file.eachFileRecurse { f ->
                if (f.name.endsWith(".class")) {
                    def className = "${packageName}.${f.name.replace('.class', '')}"
                    classNames.add(className)
                }
            }
        }
    }
    return classNames.stream()
}