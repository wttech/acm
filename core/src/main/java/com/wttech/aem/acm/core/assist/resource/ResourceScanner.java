package com.wttech.aem.acm.core.assist.resource;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = ResourceScanner.class)
public class ResourceScanner {

    /**
     * Suggest the node and children examples:
     * <p>
     * / => root node suggestion (with node type etc), and its children /apps, /content, etc (exception)
     * /content => only content node suggestion
     * /content/ => only content node children
     * /content/mysite => only mysite node suggestion
     * /content/mysite/ => only mysite node children
     * /content/mys => suggestion on /content/ children whose names start with "mys"
     * etc.
     */
    public Stream<Resource> forPattern(ResourceResolver resolver, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return Stream.empty();
        }

        if (pattern.endsWith("/")) {
            return getChildren(resolver, pattern);
        } else {
            int lastSlashIndex = pattern.lastIndexOf('/');
            if (lastSlashIndex == -1) {
                Resource resource = resolver.getResource(pattern);
                return resource == null ? Stream.empty() : Stream.of(resource);
            }
            String directory = pattern.substring(0, lastSlashIndex);
            String filename = pattern.substring(lastSlashIndex + 1);
            Stream<Resource> children = getChildren(resolver, directory);
            return children.filter(resource -> resource.getName().startsWith(filename));
        }
    }

    // Helper function to retrieve resource's children as a stream
    private Stream<Resource> getChildren(ResourceResolver resolver, String path) {
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            return Stream.empty();
        }
        Iterable<Resource> children = resource.getChildren();
        return StreamSupport.stream(children.spliterator(), false);
    }
}
