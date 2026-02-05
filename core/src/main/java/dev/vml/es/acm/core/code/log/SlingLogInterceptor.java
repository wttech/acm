package dev.vml.es.acm.core.code.log;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hooks into Sling Commons Log via AppenderTracker — registers Appender as OSGi service.
 *
 * <p>Uses reflection to avoid compile-time Logback dependencies (Cloud Manager scanner compatible).
 * Relies on internal Sling API, but stable since AEM 6.5.0 — the most pragmatic solution for
 * log interception across all supported AEM versions.</p>
 *
 * @see <a href="https://sling.apache.org/documentation/development/logging.html">Sling Logging</a>
 * @see <a href="https://github.com/apache/sling-org-apache-sling-commons-log/blob/master/src/main/java/org/apache/sling/commons/log/logback/internal/AppenderTracker.java">AppenderTracker</a>
 */
@Component(service = LogInterceptor.class)
public class SlingLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(SlingLogInterceptor.class);

    private static final String APPENDER_NAME = "ACM-SlingLogInterceptor";

    private static final String PROP_LOGGERS = "loggers";

    @Reference
    private DynamicClassLoaderManager classLoaderManager;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public boolean isAvailable() {
        if (classLoaderManager == null || bundleContext == null) {
            return false;
        }
        try {
            ClassLoader cl = classLoaderManager.getDynamicClassLoader();
            if (cl == null) {
                return false;
            }
            cl.loadClass(LogbackAppenderFactory.LOGBACK_LOGGER_CONTEXT);
            cl.loadClass(LogbackAppenderFactory.LOGBACK_APPENDER);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            LOG.debug("Cannot check Sling log interceptor availability", e);
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Cannot attach - invalid parameters");
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.warn("Sling log interceptor not available");
            return () -> {};
        }
        try {
            return doAttach(listener, loggerNames);
        } catch (Exception e) {
            LOG.error("Failed to attach Sling log interceptor", e);
            return () -> {};
        }
    }

    private Handle doAttach(Consumer<LogMessage> listener, String[] loggerNames) throws Exception {
        LogbackAppenderFactory factory = new LogbackAppenderFactory(classLoaderManager.getDynamicClassLoader());
        List<String> loggerList = Arrays.asList(loggerNames);
        Object appender = factory.createAppender(APPENDER_NAME, listener, loggerList);

        factory.setContext(appender, factory.getLoggerContext());
        factory.start(appender);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(PROP_LOGGERS, loggerNames);

        @SuppressWarnings("rawtypes")
        ServiceRegistration reg = bundleContext.registerService(LogbackAppenderFactory.LOGBACK_APPENDER, appender, props);
        LOG.debug("Registered for loggers: {}", loggerList);

        return () -> {
            try {
                reg.unregister();
                factory.stop(appender);
            } catch (Exception e) {
                LOG.warn("Failed to unregister", e);
            }
        };
    }
}
