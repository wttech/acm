package dev.vml.es.acm.core.osgi;

import dev.vml.es.acm.core.util.StreamUtils;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = OsgiScanner.class)
public class OsgiScanner {

    private static final Logger LOG = LoggerFactory.getLogger(OsgiScanner.class);

    private static final String BUNDLE_WIRING_PACKAGE = "osgi.wiring.package";

    private BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(getClass()).getBundleContext();
    }

    public Stream<Bundle> scanBundles() {
        return Arrays.stream(getBundleContext().getBundles());
    }

    public Stream<ClassInfo> scanExportedClasses() {
        return scanBundles().filter(this::isBundleOrFragmentReady).flatMap(this::scanExportedClasses);
    }

    public int computeBundlesHashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        Arrays.stream(getBundleContext().getBundles())
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

    public Bundle getSystemBundle() {
        return getBundleContext().getBundle(0);
    }

    public Stream<String> getSystemExportedPackages() {
        return findExportedPackages(getSystemBundle());
    }

    public boolean isFragment(Bundle bundle) {
        return bundle.getHeaders().get("Fragment-Host") != null;
    }

    public boolean isBundleOrFragmentReady(Bundle bundle) {
        return isBundleActive(bundle) || (isFragment(bundle) && isBundleResolved(bundle));
    }

    private Stream<String> findExportedPackages(Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Stream.empty();
        }
        return bundleWiring.getCapabilities(BUNDLE_WIRING_PACKAGE).stream()
                .map(c -> (String) c.getAttributes().get(BUNDLE_WIRING_PACKAGE));
    }

    private Stream<ClassInfo> scanExportedClasses(Bundle bundle) {
        return findExportedPackages(bundle).flatMap(pkg -> findClasses(bundle, pkg));
    }

    public Stream<ClassInfo> findClasses(Bundle bundle, String packageName) {
        try {
            Enumeration<URL> resources = bundle.findEntries(packageName.replace('.', '/'), "*.class", false);
            if (resources == null) {
                return Stream.empty();
            }

            return StreamUtils.asStream(resources)
                    .map(u -> normalizeClassName(StringUtils.removeStart(u.getFile(), "/"))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .filter(className -> isDirectChildOfPackage(className, packageName))
                    .map(c -> new ClassInfo(c, bundle, null));
        } catch (Exception e) {
            LOG.error("Error scanning classes in bundle '{}'", bundle.getSymbolicName(), e);
            return Stream.empty();
        }
    }

    private boolean isDirectChildOfPackage(String className, String packageName) {
        String classPackage = className.substring(0, className.lastIndexOf('.'));
        return classPackage.equals(packageName);
    }

    public Optional<String> normalizeClassName(String fileName) {
        return Optional.ofNullable(fileName)
                .map(f -> StringUtils.removeEnd(f, ".class").replace('/', '.'))
                .filter(f -> !f.matches(".*\\$\\d+.*") && !f.endsWith("package-info"))
                .map(f -> f.replace('$', '.'));
    }
}
