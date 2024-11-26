package com.wttech.aem.contentor.core.assist;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component(immediate = true, service = OsgiScanner.class)
public class OsgiScanner {

	private static final Logger LOG = LoggerFactory.getLogger(OsgiScanner.class);

	@Reference
	private transient DynamicClassLoaderManager classLoaderManager;

	private transient BundleContext bundleContext;

	private final Map<String, List<String>> bundleClasses = new ConcurrentHashMap<>();


	@Activate
	protected void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;

		// TODO do async warmup
		scanBundles();
	}

	private int bundleChecksum(Bundle bundle) {
		return new HashCodeBuilder()
				.append(bundle.getSymbolicName())
				.append(bundle.getLastModified())
				.append(bundle.getVersion())
				.toHashCode();
	}

	private void scanBundles() {
		for (Bundle bundle : bundleContext.getBundles()) {
			if (isBundleOrFragmentReady(bundle)) {
				final List<String> classes = scanClasses(bundle);
				bundleClasses.put(bundle.getSymbolicName(), classes);
			}
		}
	}

	private boolean isBundleOrFragmentReady(Bundle bundle) {
		return bundle.getState() == Bundle.ACTIVE
				|| (bundle.getState() == Bundle.RESOLVED && bundle.getHeaders().get("Fragment-Host") != null);
	}

	private List<String> scanClasses(Bundle bundle) {
		List<String> result = new LinkedList<>();
		ClassLoader classLoader = classLoaderManager.getDynamicClassLoader();
		Enumeration<URL> classUrls = toUrls(bundle);
		if (classUrls != null) {
			while (classUrls.hasMoreElements()) {
				final URL url = classUrls.nextElement();
				final String rawClassName = toRawClassName(url);
				if (!isLoadableClass(classLoader, rawClassName)) {
					continue;
				}
				final String stdClassName = toStdClassName(rawClassName);
				if (!isImportableClass(stdClassName)) {
					continue;
				}
				result.add(stdClassName);
			}
		}
		return result;
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

	private boolean isLoadableClass(ClassLoader classLoader, String className) {
		try {
			classLoader.loadClass(className);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	private boolean isImportableClass(String className) {
		// Exclude anonymous classes and package-info files
		return !className.matches(".*\\$\\d+.*") && !className.endsWith("package-info");
	}

	public Stream<BundleClass> allClasses() {
		return bundleClasses.entrySet().stream().flatMap(e -> {
			final String bundleSymbolicName = e.getKey();
			final List<String> classes = e.getValue();
			return classes.stream().map(c -> new BundleClass(c, bundleSymbolicName));
		});
	}

}
