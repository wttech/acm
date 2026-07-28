package dev.vml.es.acm.core.repo;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

public interface CommitPolicy {

    boolean isAutoCommit();

    void commit(String context);

    static CommitPolicy of(ResourceResolver resourceResolver, boolean autoCommit) {
        return new CommitPolicy() {
            @Override
            public boolean isAutoCommit() {
                return autoCommit;
            }

            @Override
            public void commit(String context) {
                if (!autoCommit) {
                    return;
                }
                try {
                    resourceResolver.commit();
                } catch (PersistenceException e) {
                    throw new RepoException(
                            String.format("Cannot commit changes to repository while %s!", context), e);
                }
            }
        };
    }
}
