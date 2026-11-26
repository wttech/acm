package dev.vml.es.acm.core.instance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;

class FelixTaggedHealthChecksTest {

    private final BundleContext bundleContext = MockOsgi.newBundleContext();

    private final HealthCheckExecutor executor = mock(HealthCheckExecutor.class);

    @Test
    void shouldReportOnlyCriticalResultsOfTaggedChecks() {
        HealthCheckExecutionResult requiredPathsCritical =
                result("Required paths check", Result.Status.CRITICAL, "Connection refused");
        HealthCheckExecutionResult slowLoginWarning = result("Login page check", Result.Status.WARN, "Slow response");
        when(executor.execute(any(HealthCheckSelector.class), any(HealthCheckExecutionOptions.class)))
                .thenReturn(Arrays.asList(requiredPathsCritical, slowLoginWarning));

        List<HealthIssue> issues = activate().findCriticalIssues(new String[] {"systemhealth"});

        assertEquals(1, issues.size());
        assertEquals(HealthIssueSeverity.CRITICAL, issues.get(0).getSeverity());
        assertTrue(issues.get(0).getIssue().contains("Required paths check"));
        assertTrue(issues.get(0).getDetails().contains("Connection refused"));
    }

    @Test
    void shouldDescribeCriticalResultWithoutDebugEntries() {
        ResultLog log = new ResultLog();
        log.add(new ResultLog.Entry("Checking http://localhost/libs/granite/core/content/login.html", true));
        log.add(new ResultLog.Entry(Result.Status.CRITICAL, "Connection refused"));
        HealthCheckExecutionResult requiredPathsCritical = result("Required paths check", new Result(log));
        when(executor.execute(any(HealthCheckSelector.class), any(HealthCheckExecutionOptions.class)))
                .thenReturn(Arrays.asList(requiredPathsCritical));

        List<HealthIssue> issues = activate().findCriticalIssues(new String[] {"systemhealth"});

        assertEquals("Connection refused", issues.get(0).getDetails());
    }

    @Test
    void shouldSelectChecksByConfiguredTags() {
        when(executor.execute(any(HealthCheckSelector.class), any(HealthCheckExecutionOptions.class)))
                .thenReturn(Arrays.asList());

        activate().findCriticalIssues(new String[] {"systemhealth", "custom"});

        ArgumentCaptor<HealthCheckSelector> selector = ArgumentCaptor.forClass(HealthCheckSelector.class);
        ArgumentCaptor<HealthCheckExecutionOptions> options =
                ArgumentCaptor.forClass(HealthCheckExecutionOptions.class);
        verify(executor).execute(selector.capture(), options.capture());
        assertArrayEquals(
                new String[] {"systemhealth", "custom"}, selector.getValue().tags());
        assertTrue(options.getValue().isCombineTagsWithOr());
    }

    private TaggedHealthChecks activate() {
        bundleContext.registerService(HealthCheckExecutor.class, executor, null);
        FelixTaggedHealthChecks checks = new FelixTaggedHealthChecks();
        MockOsgi.injectServices(checks, bundleContext);
        MockOsgi.activate(checks, bundleContext);
        return checks;
    }

    private HealthCheckExecutionResult result(String checkName, Result.Status status, String message) {
        return result(checkName, new Result(status, message));
    }

    private HealthCheckExecutionResult result(String checkName, Result checkResult) {
        HealthCheckMetadata metadata = mock(HealthCheckMetadata.class);
        when(metadata.getName()).thenReturn(checkName);
        HealthCheckExecutionResult result = mock(HealthCheckExecutionResult.class);
        when(result.getHealthCheckMetadata()).thenReturn(metadata);
        when(result.getHealthCheckResult()).thenReturn(checkResult);
        return result;
    }
}
