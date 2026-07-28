package dev.vml.es.acm.core.repo;

public interface CommitPolicy {

    boolean isAutoCommit();

    void commit(String context);
}
