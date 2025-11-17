package dev.vml.es.acm.core.mock;

import dev.vml.es.acm.core.repo.RepoException;
import dev.vml.es.acm.core.script.ScriptType;
import dev.vml.es.acm.core.util.ResourceSpliterator;
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

    public boolean checkResource(Resource resource) {
        return resource.getName().endsWith(".groovy") && resource.isResourceType(JcrConstants.NT_FILE);
    }

    private Optional<Resource> findResource(String subPath) {
        return Optional.ofNullable(resolver.getResource(String.format("%s/%s", ScriptType.MOCK.root(), subPath)));
    }

    public Stream<Mock> findAll() throws MockException {
        return ResourceSpliterator.stream(getRoot()).filter(this::checkResource).map(Mock::new);
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

    public Resource getRoot() throws RepoException {
        Resource root = resolver.getResource(ScriptType.MOCK.root());
        if (root == null) {
            throw new RepoException(String.format("Mock root does not exist at path '%s'!", ScriptType.MOCK.root()));
        }
        return root;
    }
}
