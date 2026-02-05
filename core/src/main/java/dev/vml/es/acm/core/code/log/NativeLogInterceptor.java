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
 * Registers an Appender as OSGi service with "loggers" property.
 * Sling Commons Log automatically attaches it to specified loggers.
 * Available on AEM 6.5+ where Sling Commons Log >= 5.0.0 is present.
 */
@Component(service = LogInterceptor.class, property = "type=" + LogInterceptor.TYPE_NATIVE)
public class NativeLogInterceptor implements LogInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(NativeLogInterceptor.class);

    private static final String APPENDER_NAME = "ACM-NativeLogInterceptor";
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
            LOG.debug("Unexpected error checking native log interceptor availability", e);
            return false;
        }
    }

    @Override
    public Handle attach(Consumer<LogMessage> listener, String... loggerNames) {
        if (listener == null || loggerNames == null || loggerNames.length == 0) {
            LOG.warn("Native log interceptor cannot attach - invalid parameters: listener={}, loggerNames={}",
                    listener, loggerNames);
            return () -> {};
        }
        if (!isAvailable()) {
            LOG.warn("Native log interceptor is not available - Logback/Sling Commons Log not found");
            return () -> {};
        }

        try {
            return doAttach(listener, loggerNames);
        } catch (Exception e) {
            LOG.error("Failed to attach native log interceptor", e);
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

        LOG.debug("Native log interceptor registered as OSGi service for loggers: {}", loggerNameList);

        return () -> {
            try {
                registration.unregister();
                factory.stop(appender);
                LOG.debug("Native log interceptor unregistered");
            } catch (Exception e) {
                LOG.warn("Failed to unregister native log interceptor", e);
            }
        };
    }
}
