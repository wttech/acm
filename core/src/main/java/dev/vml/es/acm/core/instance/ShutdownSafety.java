package dev.vml.es.acm.core.instance;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = ShutdownSafety.class)
@Designate(ocd = ShutdownSafety.Config.class)
public class ShutdownSafety {

    @ObjectClassDefinition(
            name = "AEM Content Manager - Instance Shutdown Safety (experimental)",
            description = "Experimental. Lets an automatic script stopped by an instance shutdown (e.g. a pod replaced"
                    + " during a rolling deployment) run again once on an instance that keeps running.")
    public @interface Config {

        @AttributeDefinition(
                name = "Enabled",
                description = "Automatic scripts get a lock that another instance can take over once the instance"
                        + " running them is gone. When an instance leaves the cluster, the leader boots automatic"
                        + " scripts again 30 seconds later (or after the boot delay when that is longer). A boot"
                        + " waits while the instance is shutting down (see below).")
        boolean enabled() default false;

        @AttributeDefinition(
                name = "Health Check Tags",
                description = "A boot waits while a health check with one of these tags is CRITICAL. AEM as a Cloud"
                        + " Service uses 'systemhealth'. Empty turns the wait off.")
        String[] healthCheckTags() default {"systemhealth"};

        @AttributeDefinition(
                name = "Shutdown Marker File",
                description = "A boot waits while this file exists. AEM as a Cloud Service uses"
                        + " '/var/run/aem/shutdown'. Empty turns the check off.")
        String shutdownMarkerFile() default "";

        @AttributeDefinition(
                name = "Lock Alive Timeout",
                description = "Time in milliseconds a run may stay silent before it counts as dead. A run on another"
                        + " instance that is still in the cluster never counts as dead. Minimum 6000.")
        long lockAliveTimeout() default 30_000L;

        @AttributeDefinition(
                name = "Lock Attempt Limit",
                description = "How many times an automatic script may start under the same lock, the first run"
                        + " included. Minimum 2.")
        long lockAttemptLimit() default 3L;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownSafety.class);

    public static final long MIN_LOCK_ALIVE_TIMEOUT = 6_000L;

    public static final long MIN_LOCK_ATTEMPT_LIMIT = 2L;

    private volatile Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        if (config.lockAliveTimeout() < MIN_LOCK_ALIVE_TIMEOUT) {
            LOG.warn(
                    "Lock alive timeout {} ms is too short. Using the minimum of {} ms.",
                    config.lockAliveTimeout(),
                    MIN_LOCK_ALIVE_TIMEOUT);
        }
        if (config.lockAttemptLimit() < MIN_LOCK_ATTEMPT_LIMIT) {
            LOG.warn(
                    "Lock attempt limit {} is too low. Using the minimum of {}.",
                    config.lockAttemptLimit(),
                    MIN_LOCK_ATTEMPT_LIMIT);
        }
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public String[] getHealthCheckTags() {
        return config.healthCheckTags();
    }

    /** An instance that is about to stop must not start boot scripts. */
    public ShutdownMarkerFile.State checkShutdownMarkerFile() {
        String markerFile = getShutdownMarkerFile();
        if (!isEnabled() || markerFile.isEmpty()) {
            return ShutdownMarkerFile.State.ABSENT;
        }
        return new ShutdownMarkerFile(markerFile).check();
    }

    public String getShutdownMarkerFile() {
        return StringUtils.trimToEmpty(config.shutdownMarkerFile());
    }

    public long getLockAliveTimeout() {
        return Math.max(MIN_LOCK_ALIVE_TIMEOUT, config.lockAliveTimeout());
    }

    public long getLockAttemptLimit() {
        return Math.max(MIN_LOCK_ATTEMPT_LIMIT, config.lockAttemptLimit());
    }
}
