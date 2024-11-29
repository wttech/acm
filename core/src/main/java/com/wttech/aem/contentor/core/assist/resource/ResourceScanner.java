package com.wttech.aem.contentor.core.assist.resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component(immediate = true, service = ResourceScanner.class)
public class ResourceScanner {

	/**
	 * Suggest the node and children if path is having trailing slash, examples:
	 *
	 * / => root node suggestion (with node type etc), and its children /apps, /content, etc (exception)
	 * /content => only content node suggestion
	 * /content/ => only content node children
	 * /content/mysite => only mysite node suggestion
	 * /content/mysite/ => only mysite node children
	 * etc.
	 */
	public Stream<Resource> forPattern(ResourceResolver resolver, String pattern) {
		if (StringUtils.isBlank(pattern)) {
			return Stream.empty();
		}

		Resource resource = resolver.getResource(pattern);
		if (resource == null) {
			return Stream.empty();
		}

		if (pattern.endsWith("/")) {
			return StreamSupport.stream(resource.getChildren().spliterator(), false);
		} else {
			return Stream.of(resource);
		}
	}

}
