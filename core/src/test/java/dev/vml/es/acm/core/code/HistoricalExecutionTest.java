package dev.vml.es.acm.core.code;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.vml.es.acm.core.util.JsonUtils;
import java.util.Date;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

class HistoricalExecutionTest {

    private final ResourceResolver resolver =
            MockSling.newResourceResolver(ResourceResolverType.RESOURCERESOLVER_MOCK, MockOsgi.newBundleContext());

    @Test
    void shouldStoreShutdownCause() {
        Map<String, Object> props = HistoricalExecution.toMap(execution(AbortCause.INSTANCE_SHUTDOWN));

        assertEquals("INSTANCE_SHUTDOWN", props.get("abortCause"));
    }

    @Test
    void shouldStoreNothingWithoutCause() {
        Map<String, Object> props = HistoricalExecution.toMap(execution(null));

        assertFalse(props.containsKey("abortCause"));
    }

    @Test
    void shouldReadStoredCause() throws Exception {
        HistoricalExecution historical = save(execution(AbortCause.INSTANCE_SHUTDOWN));

        assertEquals(AbortCause.INSTANCE_SHUTDOWN, historical.getAbortCause());
    }

    @Test
    void shouldReadOldEntryWithoutCause() throws Exception {
        HistoricalExecution historical = save(execution(null));

        assertNull(historical.getAbortCause());
    }

    @Test
    void shouldShowCauseInJson() throws Exception {
        HistoricalExecution historical = save(execution(AbortCause.INSTANCE_SHUTDOWN));

        assertTrue(JsonUtils.writeToString(historical).contains("\"abortCause\":\"INSTANCE_SHUTDOWN\""));
    }

    @Test
    void shouldKeepJsonUnchangedWithoutCause() throws Exception {
        HistoricalExecution historical = save(execution(null));

        assertFalse(JsonUtils.writeToString(historical).contains("abortCause"));
    }

    private HistoricalExecution save(ContextualExecution execution) throws Exception {
        Resource root = resolver.getResource("/");
        Resource entry = resolver.create(root, "entry", HistoricalExecution.toMap(execution));
        return new HistoricalExecution(entry);
    }

    private ContextualExecution execution(AbortCause abortCause) {
        ContextualExecution execution = mock(ContextualExecution.class, RETURNS_DEEP_STUBS);
        when(execution.getId()).thenReturn("2026/11/16/12/56/exec_1");
        when(execution.getContext().getUserId()).thenReturn("acm-content-service");
        when(execution.getStatus()).thenReturn(ExecutionStatus.ABORTED);
        when(execution.getStartDate()).thenReturn(new Date(0));
        when(execution.getEndDate()).thenReturn(new Date(1000));
        when(execution.getInputs()).thenReturn(new InputValues());
        when(execution.getOutputs()).thenReturn(new OutputValues());
        when(execution.getExecutable()).thenReturn(new Code("/conf/acm/settings/script/automatic/test.groovy", "x"));
        when(execution.readOutput()).thenReturn(null);
        when(execution.getAbortCause()).thenReturn(abortCause);
        return execution;
    }
}
