package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import com.wttech.aem.contentor.core.instance.HealthChecker;
import com.wttech.aem.contentor.core.util.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = {ExecutionQueue.class, JobExecutor.class},
    property = {JobExecutor.PROPERTY_TOPICS + "=" + ExecutionQueue.TOPIC})
public class ExecutionQueue implements JobExecutor {

  public static final String TOPIC = "com/wttech/aem/contentor/ExecutionQueue";

  public static final String TMP_DIR = "contentor";

  private static final Logger LOG = LoggerFactory.getLogger(ExecutionQueue.class);

  // TODO make this configurable
  private static final long EXECUTE_POLL_INTERVAL = 1000;

  // TODO make this configurable
  private static final long CLEAN_POLL_DELAY = 3000;

  @Reference private JobManager jobManager;

  @Reference private HealthChecker healthChecker;

  @Reference private ResourceResolverFactory resourceResolverFactory;

  @Reference private Executor executor;

  private ExecutorService jobAsyncExecutor;

  static Optional<String> readFile(String jobId, FileType fileType) throws ContentorException {
    Path path = filePath(jobId, fileType);
    if (!path.toFile().exists()) {
      return Optional.empty();
    }

    try (InputStream input = Files.newInputStream(path)) {
      return Optional.ofNullable(IOUtils.toString(input, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new ContentorException(String.format("Cannot read output file for job '%s'", jobId), e);
    }
  }

  static Path filePath(String jobId, FileType kind) {
    File dir = FileUtils.getTempDirectory().toPath().resolve(TMP_DIR).toFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dir.toPath()
        .resolve(
            String.format(
                "%s_%s.log", StringUtils.replace(jobId, "/", "-"), kind.name().toLowerCase()));
  }

  public ExecutionContext createContext(Executable executable, ResourceResolver resourceResolver) {
    return executor.createContext(executable, resourceResolver);
  }

  public Optional<Execution> submit(Executable executable) throws ContentorException {
    Job job = jobManager.addJob(TOPIC, Code.toJobProps(executable));
    if (job == null) {
      return Optional.empty();
    }
    return read(job.getId());
  }

  public Optional<Execution> read(String jobId) throws ContentorException {
    Job job = jobManager.getJobById(jobId);
    if (job == null) {
      return Optional.empty();
    }
    return Optional.of(new QueuedExecution(job));
  }

  public void stop(String jobId) {
    jobManager.stopJobById(jobId);
  }

  @Activate
  protected void activate() {
    jobAsyncExecutor = Executors.newSingleThreadExecutor();
  }

  @Deactivate
  protected void deactivate() {
    if (jobAsyncExecutor != null) {
      jobAsyncExecutor.shutdown();
    }
  }

  @Override
  public JobExecutionResult process(Job job, JobExecutionContext context) {
    Executable executable = Code.fromJob(job);
    if (!healthChecker.isHealthy()) {
      LOG.warn("Failing execution '{}' - instance is not healthy.", executable);
      return context.result().failed();
    }

    LOG.info("Executing asynchronously '{}'", executable);

    Future<Execution> future =
        jobAsyncExecutor.submit(
            () -> {
              try {
                return executeAsync(executable, job);
              } catch (Throwable e) {
                throw new ContentorException(
                    String.format(
                        "Executing executable '%s' asynchronously failed internally '{}'",
                        executable.getId()),
                    e);
              }
            });

    while (!future.isDone()) {
      if (context.isStopped() || Thread.currentThread().isInterrupted()) {
        future.cancel(true);
        LOG.info("Job '{}' is cancelling", executable);
        break;
      }
      try {
        Thread.sleep(EXECUTE_POLL_INTERVAL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.info("Job '{}' was interrupted", executable);
        return context.result().cancelled();
      }
    }

    try {
      Execution execution = future.get();
      String message = QueuedExecution.composeJobResultMessage(execution);

      if (execution.getStatus() == ExecutionStatus.SKIPPED) {
        LOG.info("Job '{}' is skipped", executable);
        return context.result().message(message).cancelled();
      } else {
        LOG.info("Executed asynchronously '{}'", executable);
        return context.result().message(message).succeeded();
      }
    } catch (CancellationException e) {
      LOG.info("Job '{}' is cancelled", executable);
      return context.result().cancelled();
    } catch (Exception e) {
      LOG.error("Error executing asynchronously '{}'", executable, e);
      return context.result().failed();
    } finally {
      jobAsyncExecutor.submit(
          () -> {
            try {
              cleanAsync(executable, job);
            } catch (Throwable e) {
              LOG.error("Cleaning up asynchronously failed '{}'", executable, e);
            }
          });
    }
  }

  private Execution executeAsync(Executable executable, Job job) throws ContentorException {
    try (ResourceResolver resolver = ResourceUtils.serviceResolver(resourceResolverFactory);
        OutputStream outputStream = Files.newOutputStream(filePath(job.getId(), FileType.OUTPUT))) {
      ExecutionContext context = executor.createContext(executable, resolver);
      context.setOutputStream(outputStream);
      return executor.execute(executable, context);
    } catch (LoginException e) {
      throw new ContentorException(
          String.format(
              "Cannot access repository while executing '%s' in job '%s'",
              executable.getId(), job.getId()),
          e);
    } catch (IOException e) {
      throw new ContentorException(
          String.format(
              "Cannot write to files for executable '%s' in job '%s'",
              executable.getId(), job.getId()),
          e);
    }
  }

  private void cleanAsync(Executable executable, Job job) {
    try {
      Thread.sleep(CLEAN_POLL_DELAY);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.info("Cleaning up job '{}' was interrupted", job.getId());
      return;
    }

    try {
      Files.deleteIfExists(filePath(job.getId(), FileType.OUTPUT));
      Files.deleteIfExists(filePath(job.getId(), FileType.ERROR));
    } catch (IOException e) {
      LOG.error(
          "Cannot delete files for executable '{}' in job '{}'",
          executable.getId(),
          job.getId(),
          e);
    }
  }

  public enum FileType {
    OUTPUT,
    ERROR
  }
}
