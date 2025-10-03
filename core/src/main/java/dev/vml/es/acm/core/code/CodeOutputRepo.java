package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.repo.RepoChunks;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeOutputRepo implements CodeOutput {

    private static final Logger LOG = LoggerFactory.getLogger(CodeOutputRepo.class);

    private static final int SCHEDULER_TERMINATION_TIMEOUT_SECONDS = 5;

    private SpaSettings spaSettings;

    private final String executionId;

    private final RepoChunks repoChunks;

    private ScheduledExecutorService asyncFlushScheduler;

    public CodeOutputRepo(ResourceResolverFactory resolverFactory, SpaSettings spaSettings, String executionId) {
        this.spaSettings = spaSettings;
        this.executionId = executionId;
        this.repoChunks = new RepoChunks(
                resolverFactory,
                String.format("%s/output", ExecutionContext.varPath(executionId)),
                spaSettings.getExecutionConsoleOutputChunkSize());
    }

    @Override
    public InputStream read() {
        try {
            return repoChunks.getInputStream();
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Output repo cannot open for reading for execution ID '%s'", executionId), e);
        }
    }

    @Override
    public OutputStream write() {
        startAsyncFlush();
        return repoChunks.getOutputStream();
    }

    @Override
    public void flush() {
        try {
            repoChunks.flush();
        } catch (IOException e) {
            LOG.error("Output repo cannot flush for execution ID '{}'", executionId, e);
        }
    }

    @Override
    public void close() {
        stopAsyncFlush();
        try {
            repoChunks.close();
        } catch (IOException e) {
            LOG.error("Output repo cannot close for execution ID '{}'", executionId, e);
        }
    }

    private void startAsyncFlush() {
        if (asyncFlushScheduler == null) {
            asyncFlushScheduler = Executors.newSingleThreadScheduledExecutor();
            asyncFlushScheduler.scheduleWithFixedDelay(
                    this::flush, 0, Math.round(0.8 * spaSettings.getExecutionPollInterval()), TimeUnit.MILLISECONDS);
        }
    }

    private void stopAsyncFlush() {
        if (asyncFlushScheduler != null) {
            asyncFlushScheduler.shutdown();
            try {
                asyncFlushScheduler.awaitTermination(SCHEDULER_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
