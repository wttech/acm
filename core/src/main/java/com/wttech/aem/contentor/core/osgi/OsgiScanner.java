package com.wttech.aem.contentor.core.osgi;

import com.wttech.aem.contentor.core.util.StreamUtils;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
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
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Stream.empty();
        }

        return bundleWiring.getCapabilities(BUNDLE_WIRING_PACKAGE).stream()
                .map(capability -> (String) capability.getAttributes().get(BUNDLE_WIRING_PACKAGE))
                .flatMap(pkg -> findClasses(bundle, pkg));
    }

    private Stream<ClassInfo> findClasses(Bundle bundle, String packageName) {
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
