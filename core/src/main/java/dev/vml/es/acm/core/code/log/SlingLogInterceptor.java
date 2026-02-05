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
 * Log interceptor using Sling Commons Log AppenderTracker mechanism.
 *
 * <p>This interceptor registers a Logback Appender as an OSGi service with a "loggers" property.
 * Sling Commons Log automatically discovers and attaches it to the specified loggers via its
 * internal AppenderTracker. This is the same mechanism used by the Sling Log Support console
 * available at /system/console/slinglog.</p>
 *
 * <p>The implementation uses reflection to create the Appender proxy, avoiding compile-time
 * dependencies on Logback internal classes (ch.qos.logback.*). This ensures compatibility
 * with Adobe Cloud Manager code quality scanners.</p>
 *
 * <p>Available on AEM 6.5+ where Sling Commons Log >= 5.0.0 is present.</p>
 *
 * @see <a href="https://sling.apache.org/documentation/development/logging.html">
 *     Sling Logging Documentation</a>
 * @see <a href="https://github.com/apache/sling-org-apache-sling-commons-log/blob/master/src/main/java/org/apache/sling/commons/log/logback/internal/AppenderTracker.java">
 *     Sling AppenderTracker Implementation</a>
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
            // Check if Logback classes are available
            cl.loadClass(LogbackAppenderFactory.LOGBACK_LOGGER_CONTEXT);
            cl.loadClass(LogbackAppenderFactory.LOGBACK_APPENDER);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            LOG.debug("Unexpected error checking Sling log interceptor availability", e);
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Sling log interceptor cannot attach - invalid parameters: listener={}, loggerNames={}",
                    listener, loggerNames);
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.warn("Sling log interceptor is not available - Logback/Sling Commons Log not found");
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
        ClassLoader cl = classLoaderManager.getDynamicClassLoader();
        LogbackAppenderFactory factory = new LogbackAppenderFactory(cl);

        List<String> loggerNameList = Arrays.asList(loggerNames);
        Object appender = factory.createAppender(APPENDER_NAME, listener, loggerNameList);

        // Initialize the appender with LoggerContext
        Object loggerContext = factory.getLoggerContext();
        factory.setContext(appender, loggerContext);
        factory.start(appender);

        // Register as OSGi service - Sling AppenderTracker will pick it up
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(PROP_LOGGERS, loggerNames);

        @SuppressWarnings("rawtypes")
        ServiceRegistration registration = bundleContext.registerService(
                LogbackAppenderFactory.LOGBACK_APPENDER, appender, props);

        LOG.debug("Sling log interceptor registered as OSGi service for loggers: {}", loggerNameList);

        return () -> {
            try {
                registration.unregister();
                factory.stop(appender);
                LOG.debug("Sling log interceptor unregistered");
            } catch (Exception e) {
                LOG.warn("Failed to unregister Sling log interceptor", e);
            }
        };
    }
}
