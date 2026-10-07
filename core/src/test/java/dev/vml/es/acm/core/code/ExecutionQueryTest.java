package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExecutionQueryTest {

    @Test
    void shouldEscapeQuotesInExplicitExecutableId() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId("/conf/acm/settings/script/x' OR s.[status] <> 'y");

        assertTrue(query.toSql().contains("s.[executableId] = '/conf/acm/settings/script/x'' OR s.[status] <> ''y'"));
    }

    @Test
    void shouldEscapeQuotesInLikeExecutableId() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId("x' OR '1'='1");

        assertTrue(query.toSql().contains("s.[executableId] LIKE '%x'' OR ''1''=''1%'"));
    }

    @Test
    void shouldEscapeQuotesInId() {
        ExecutionQuery query = new ExecutionQuery();
        query.setId("1' OR '1'='1");

        assertTrue(query.toSql().contains("s.[id] = '1'' OR ''1''=''1'"));
    }
}
