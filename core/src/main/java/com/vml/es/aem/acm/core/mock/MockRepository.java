package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.script.ScriptType;
import com.vml.es.aem.acm.core.util.ResourceSpliterator;
import com.vml.es.aem.acm.core.util.ResourceUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;

public class MockRepository {

    public static final String CORE_DIR = "core";

    public static final String FAIL_PATH = CORE_DIR + "/fail.groovy";

    public static final String MISSING_PATH = CORE_DIR + "/missing.groovy";

    public static final List<String> SPECIAL_PATHS = Arrays.asList(FAIL_PATH, MISSING_PATH);

    private final ResourceResolver resolver;

    public MockRepository(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public Resource getOrCreateRoot() throws AcmException {
        return ResourceUtils.makeFolders(resolver, ScriptType.MOCK.root());
    }

    public boolean checkResource(Resource resource) {
        return resource.getName().endsWith(".groovy") && resource.isResourceType(JcrConstants.NT_FILE);
    }

    private Optional<Resource> findResource(String subPath) {
        return Optional.ofNullable(resolver.getResource(String.format("%s/%s", ScriptType.MOCK.root(), subPath)));
    }

    public Stream<Mock> findAll() throws MockException {
        return ResourceSpliterator.stream(getOrCreateRoot())
                .filter(this::checkResource)
                .map(Mock::new);
    }

    public Optional<Mock> find(String subPath) {
        return findResource(subPath).filter(this::checkResource).map(Mock::new);
    }

    public Optional<Mock> findSpecial(String subPath) {
        return findResource(subPath).map(Mock::new);
    }

    public boolean isSpecial(String id) {
        return SPECIAL_PATHS.stream().anyMatch(n -> StringUtils.endsWith(id, "/" + n));
    }
}
