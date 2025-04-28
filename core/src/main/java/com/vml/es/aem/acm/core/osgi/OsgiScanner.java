package com.vml.es.aem.acm.core.osgi;

import com.vml.es.aem.acm.core.util.StreamUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = OsgiScanner.class)
public class OsgiScanner {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiScanner.class);

    private static final String BUNDLE_WIRING_PACKAGE = "osgi.wiring.package";

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Stream<Bundle> scanBundles() {
        return Arrays.stream(bundleContext.getBundles());
    }

    public Stream<ClassInfo> scanClasses() {
        return scanBundles().filter(this::isBundleOrFragmentReady).flatMap(this::scanClasses);
    }

    public int computeBundlesHashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        Arrays.stream(bundleContext.getBundles())
                .filter(this::isBundleOrFragmentReady)
                .forEach(bundle -> {
                    builder.append(bundle.getSymbolicName());
                    builder.append(bundle.getVersion());
                    builder.append(bundle.getLastModified());
                });
        return builder.toHashCode();
    }

    public boolean isBundleActive(Bundle bundle) {
        return bundle.getState() == Bundle.ACTIVE;
    }

    public boolean isBundleResolved(Bundle bundle) {
        return bundle.getState() == Bundle.RESOLVED;
    }

    public boolean isFragment(Bundle bundle) {
        return bundle.getHeaders().get("Fragment-Host") != null;
    }

    public boolean isBundleOrFragmentReady(Bundle bundle) {
        return isBundleActive(bundle) || (isFragment(bundle) && isBundleResolved(bundle));
    }

    private Stream<ClassInfo> scanClasses(Bundle bundle) {
        return readPackages(bundle)
                .flatMap(pkg ->
                        isSystemBundle(bundle) ? findSystemClasses(bundle, pkg) : findRegularClasses(bundle, pkg));
    }

    private boolean isSystemBundle(Bundle bundle) {
        return bundle.getBundleId() == 0;
    }

    private Stream<String> readPackages(Bundle bundle) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring == null) {
            return Stream.empty();
        }
        return wiring.getCapabilities(BUNDLE_WIRING_PACKAGE).stream()
                .map(c -> (String) c.getAttributes().get(BUNDLE_WIRING_PACKAGE));
    }

    private Stream<ClassInfo> findRegularClasses(Bundle bundle, String packageName) {
        try {
            Enumeration<URL> resources = bundle.findEntries(packageName.replace('.', '/'), "*.class", false);
            if (resources == null) {
                return Stream.empty();
            }

            return StreamUtils.asStream(resources)
                    .map(this::toRawClassName)
                    .filter(className -> isDirectChildOfPackage(className, packageName))
                    .filter(this::isImportableClass)
                    .map(this::toStdClassName)
                    .map(c -> new ClassInfo(c, bundle));
        } catch (Exception e) {
            LOG.error("Error scanning classes in bundle '{}'", bundle.getSymbolicName(), e);
            return Stream.empty();
        }
    }

    private Stream<ClassInfo> findSystemClasses(Bundle bundle, String packageName) {
        List<ClassInfo> classInfos = new ArrayList<>();
        String path = packageName.replace('.', '/');
        String[] classpathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));

        for (String entry : classpathEntries) {
            File jarFile = new File(entry);
            if (jarFile.exists() && jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(jarFile)) {
                    jar.stream()
                            .filter(e ->
                                    e.getName().startsWith(path) && e.getName().endsWith(".class"))
                            .map(e -> e.getName().replace("/", ".").replace(".class", ""))
                            .forEach(className -> classInfos.add(new ClassInfo(className, bundle)));
                } catch (IOException e) {
                    LOG.error("Error reading JAR file '{}'", jarFile.getAbsolutePath(), e);
                }
            }
        }

        return classInfos.stream();
    }

    private boolean isDirectChildOfPackage(String className, String packageName) {
        String classPackage = className.substring(0, className.lastIndexOf('.'));
        return classPackage.equals(packageName);
    }

    private String toRawClassName(URL url) {
        final String f = url.getFile();
        final String cn = f.substring(1, f.length() - ".class".length());
        return cn.replace('/', '.').replace("$", ".");
    }

    private String toStdClassName(String rawClassName) {
        return rawClassName;
    }

    private boolean isImportableClass(String className) {
        return !className.matches(".*\\$\\d+.*") && !className.endsWith("package-info");
    }
}
