package com.wttech.aem.contentor.core.assist;

import com.wttech.aem.contentor.core.util.SearchUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

@Component(immediate = true, service = OsgiScanner.class)
public class OsgiScanner {

	private static final Logger LOG = LoggerFactory.getLogger(OsgiScanner.class);

	@Reference
	private transient DynamicClassLoaderManager classLoaderManager;

	private transient BundleContext bundleContext;

	@Activate
	protected void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public List<String> findClassNames() {
		return findClassNames(null);
	}

	public List<String> findClassNames(String word) {
		List<String> classNames = new LinkedList<>();

		ClassLoader classLoader = classLoaderManager.getDynamicClassLoader();
		int notLoadable = 0;
		int notImportable = 0;

		for (Bundle bundle : bundleContext.getBundles()) {
			Enumeration<URL> classUrls = toUrls(bundle);
			if (classUrls != null) {
				while (classUrls.hasMoreElements()) {
					final URL url = classUrls.nextElement();
					final String rawClassName = toRawClassName(url);

					if (!isLoadableClass(classLoader, rawClassName)) {
						notLoadable++;
						continue;
					}

					final String stdClassName = toStdClassName(rawClassName);
					if (!isImportableClass(stdClassName)) {
						notImportable++;
						continue;
					}

					if (word == null || SearchUtils.containsWord(word, stdClassName)) {
						classNames.add(stdClassName);
					}
				}
			}
		}

		LOG.info("Found {} class names. Not loadable: {}, not importable: {}", classNames.size(), notLoadable, notImportable);

		return classNames;
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
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private boolean isImportableClass(String className) {
		// Exclude anonymous classes
		return !className.matches(".*\\$\\d+.*");
	}
}
