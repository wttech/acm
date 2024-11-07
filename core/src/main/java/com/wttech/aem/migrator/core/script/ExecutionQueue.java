package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import com.wttech.aem.migrator.core.instance.HealthChecker;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {ExecutionQueue.class, JobConsumer.class},
        property = {JobConsumer.PROPERTY_TOPICS + "=" + ExecutionQueue.TOPIC})
public class ExecutionQueue implements JobConsumer {

    // TODO add osgi config with proper queue configuration:
    // https://sling.apache.org/documentation/bundles/apache-sling-eventing-and-job-handling.html#queue-configurations
    public static final String TOPIC = "com/wttech/aem/migrator/ExecutionQueue";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);

    @Reference
    private JobManager jobManager;

    @Reference
    private HealthChecker healthChecker;

    @Reference
    private Executor executor;

    public void add(Executable executable) throws MigratorException {
        jobManager.addJob(TOPIC, Code.toJobProps(executable));
    }

    @Override
    public JobResult process(Job job) {
        var executable = Code.fromJob(job);
        if (healthChecker.isHealthy()) {
            LOG.warn("Cancelling execution '{}' - instance is not healthy", executable);
            return JobResult.CANCEL;
        }
        LOG.info("Executing '{}'", executable);
        try {
            executor.execute(executable);
        } catch (MigratorException e) {
            LOG.error("Cannot execute '{}'", executable, e);
            return JobResult.FAILED;
        }
        LOG.info("Executed '{}'", executable);
        return JobResult.OK;
    }
}
