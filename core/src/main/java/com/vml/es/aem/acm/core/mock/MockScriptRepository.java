package com.vml.es.aem.acm.core.mock;

import com.vml.es.aem.acm.core.AcmException;
import com.vml.es.aem.acm.core.util.ResourceSpliterator;
import com.vml.es.aem.acm.core.util.ResourceUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;

public class MockScriptRepository {

    public static final String ROOT = "/conf/acm/settings/script/mock";

    public static final String CORE_DIR = "core";

    public static final String FAIL_PATH = CORE_DIR + "/fail.groovy";

    public static final String MISSING_PATH = CORE_DIR + "/missing.groovy";

    public static final List<String> SPECIAL_PATHS = Arrays.asList(FAIL_PATH, MISSING_PATH);

    private final ResourceResolver resolver;

    public MockScriptRepository(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public Resource getOrCreateRoot() throws AcmException {
        return ResourceUtils.makeFolders(resolver, ROOT);
    }

    public boolean checkResource(Resource resource) {
        return resource.getName().endsWith(".groovy") && resource.isResourceType(JcrConstants.NT_FILE);
    }

    private Optional<Resource> findResource(String subPath) {
        return Optional.ofNullable(resolver.getResource(String.format("%s/%s", ROOT, subPath)));
    }

    public Stream<MockScriptExecutable> findAll() throws MockException {
        return ResourceSpliterator.stream(getOrCreateRoot(), this::checkResource)
                .map(s -> s.adaptTo(MockScriptExecutable.class));
    }

    public Optional<MockScriptExecutable> find(String subPath) {
        return findResource(subPath).filter(this::checkResource).map(s -> s.adaptTo(MockScriptExecutable.class));
    }

    public Optional<MockScriptExecutable> findSpecial(String subPath) {
        return findResource(subPath).map(s -> s.adaptTo(MockScriptExecutable.class));
    }

    public boolean isSpecial(String id) {
        return SPECIAL_PATHS.stream().anyMatch(n -> StringUtils.endsWith(id, "/" + n));
    }
}
