package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.util.StreamUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

@Component(immediate = true, service = OsgiScanner.class)
public class OsgiScanner {

	@Reference
	private transient DynamicClassLoaderManager classLoaderManager;

	private transient BundleContext bundleContext;

	@Activate
	@Modified
	protected void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public Stream<BundleClass> scanClasses() {
		return Arrays.stream(bundleContext.getBundles())
				.filter(this::isBundleOrFragmentReady)
				.flatMap(this::scanClasses);
	}

	private boolean isBundleOrFragmentReady(Bundle bundle) {
		return bundle.getState() == Bundle.ACTIVE
				|| (bundle.getState() == Bundle.RESOLVED && bundle.getHeaders().get("Fragment-Host") != null);
	}

	private Stream<BundleClass> scanClasses(Bundle bundle) {
		return StreamUtils.asStream(toUrls(bundle))
				.map(this::toRawClassName)
				.filter(this::isLoadableClass)
				.map(this::toStdClassName)
				.filter(this::isImportableClass)
				.map(c -> new BundleClass(c, bundle.getSymbolicName()));
	}

	private Enumeration<URL> toUrls(Bundle bundle) {
		return bundle.findEntries("/", "*.class", true);
	}

	private String toRawClassName(URL url) {
		final String f = url.getFile();
		final String cn = f.substring(1, f.length() - ".class".length());

		return cn.replace('/', '.');
	}

	private String toStdClassName(String rawClassName) {
		return rawClassName.replace("$", ".");
	}

	private boolean isLoadableClass(String className) {
		try {
			classLoaderManager.getDynamicClassLoader().loadClass(className);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	private boolean isImportableClass(String className) {
		// Exclude anonymous classes and package-info files
		return !className.matches(".*\\$\\d+.*") && !className.endsWith("package-info");
	}
}
