package com.wttech.aem.contentor.core.assist.osgi;

import com.wttech.aem.contentor.core.util.SearchUtils;
import org.osgi.framework.Bundle;

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class BundleScanner {

	private final Bundle bundle;

	public BundleScanner(Bundle bundle) {
		this.bundle = bundle;
	}

	public List<String> findClassNames() {
		return findClassNames(null);
	}

	public List<String> findClassNames(String word) {
		List<String> classNames = new LinkedList<>();

		@SuppressWarnings("unchecked")
		final Enumeration<URL> classUrls = getUrls();
		if (classUrls != null) {
			while (classUrls.hasMoreElements()) {
				final URL url = classUrls.nextElement();
				final String className = toClassName(url);

				if (word == null || SearchUtils.containsWord(word, className)) {
					classNames.add(className);
				}
			}
		}

		return classNames;
	}

	private Enumeration getUrls() {
		return bundle.findEntries("/", "*.class", true);
	}

	private String toClassName(URL url) {
		final String f = url.getFile();
		final String cn = f.substring(1, f.length() - ".class".length());

		return cn.replace('/', '.');
	}
}
