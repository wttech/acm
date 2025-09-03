package dev.vml.es.acm.core.instance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;

@ExtendWith(MockitoExtension.class)
class HealthCheckerTest {

    @InjectMocks
    private HealthChecker healthChecker;

    @Mock
    private HealthChecker.Config config;

    @Mock
    private Bundle bundle;

    @Test
    void testIsBundleIgnored_NullIgnoredList() {
        when(config.bundleSymbolicNamesIgnored()).thenReturn(null);
        assertFalse(invokeIsBundleIgnored(bundle));
    }

    @Test
    void testIsBundleIgnored_EmptyIgnoredList() {
        when(config.bundleSymbolicNamesIgnored()).thenReturn(ArrayUtils.EMPTY_STRING_ARRAY);
        assertFalse(invokeIsBundleIgnored(bundle));
    }

    @Test
    void testIsBundleIgnored_MatchWildcard() {
        when(config.bundleSymbolicNamesIgnored()).thenReturn(new String[] {"com.example.*"});
        when(bundle.getSymbolicName()).thenReturn("com.example.foo");
        assertTrue(invokeIsBundleIgnored(bundle));
    }

    @Test
    void testIsBundleIgnored_NoMatch() {
        when(config.bundleSymbolicNamesIgnored()).thenReturn(new String[] {"org.other.*"});
        when(bundle.getSymbolicName()).thenReturn("com.example.foo");
        assertFalse(invokeIsBundleIgnored(bundle));
    }

    @Test
    void testIsBundleIgnored_TrimmedName() {
        when(config.bundleSymbolicNamesIgnored()).thenReturn(new String[] {"com.example.bar "});
        when(bundle.getSymbolicName()).thenReturn("com.example.bar");
        assertTrue(invokeIsBundleIgnored(bundle));
    }

    private boolean invokeIsBundleIgnored(Bundle bundle) {
        try {
            Method method = HealthChecker.class.getDeclaredMethod("isBundleIgnored", Bundle.class);
            method.setAccessible(true);
            return (boolean) method.invoke(healthChecker, bundle);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
