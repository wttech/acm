package com.wttech.aem.contentor.core.assist.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.net.URL;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.Arrays;
import com.wttech.aem.contentor.core.util.StreamUtils;
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

	public Stream<ClassInfo> scanClasses() {
		return Arrays.stream(bundleContext.getBundles())
				.filter(this::isBundleOrFragmentReady)
				.flatMap(this::scanClasses);
	}

	private boolean isBundleOrFragmentReady(Bundle bundle) {
		return bundle.getState() == Bundle.ACTIVE
				|| (bundle.getState() == Bundle.RESOLVED && bundle.getHeaders().get("Fragment-Host") != null);
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
