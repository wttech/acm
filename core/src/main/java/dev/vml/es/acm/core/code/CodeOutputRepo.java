package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.repo.RepoResource;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeOutputRepo implements CodeOutput {

    private static final Logger LOG = LoggerFactory.getLogger(CodeOutputRepo.class);

    private static final String MIME_TYPE = "text/plain";

    private static final String OUTPUT_ROOT = "output";

    private static final int SCHEDULER_TERMINATION_TIMEOUT_SECONDS = 5;

    private final ResourceResolverFactory resolverFactory;

    private SpaSettings spaSettings;

    private final String executionId;

    private final ByteArrayOutputStream buffer;

    private ScheduledExecutorService scheduler;

    public CodeOutputRepo(ResourceResolverFactory resolverFactory, SpaSettings spaSettings, String executionId) {
        this.resolverFactory = resolverFactory;
        this.spaSettings = spaSettings;
        this.executionId = executionId;
        this.buffer = new ByteArrayOutputStream();
    }

    private void startAsyncSave() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(
                    this::saveToRepo,
                    0,
                    Math.round(0.8 * spaSettings.getExecutionPollInterval()),
                    TimeUnit.MILLISECONDS);
        }
    }

    private RepoResource getFile(ResourceResolver resolver) {
        return Repo.quiet(resolver)
                .get(String.format(
                        "%s/%s/%s_output.txt",
                        AcmConstants.VAR_ROOT, OUTPUT_ROOT, StringUtils.replace(executionId, "/", "-")));
    }

    private void saveToRepo() {
        byte[] data = buffer.toByteArray();
        if (data.length == 0) {
            return;
        }

        try {
            ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
                RepoResource dataResource = getFile(resolver);
                dataResource.parent().ensureRegularFolder();
                dataResource.saveFile(MIME_TYPE, new ByteArrayInputStream(data));
            });
        } catch (Exception e) {
            LOG.error("Output repo cannot save data for execution ID '{}'", executionId, e);
        }
    }

    @Override
    public InputStream read() {
        try {
            return ResolverUtils.queryContentResolver(resolverFactory, null, resolver -> {
                RepoResource resource = getFile(resolver);
                if (!resource.exists()) {
                    return new ByteArrayInputStream(new byte[0]);
                }
                return resource.readFileAsStream();
            });
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Output repo cannot open for reading for execution ID '%s'", executionId), e);
        }
    }

    @Override
    public OutputStream write() {
        startAsyncSave();
        return buffer;
    }

    @Override
    public void flush() {
        saveToRepo();
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(SCHEDULER_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        deleteFromRepo();
    }

    private void deleteFromRepo() {
        try {
            ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
                RepoResource fileResource = getFile(resolver);
                if (fileResource.exists()) {
                    fileResource.delete();
                }
            });
        } catch (Exception e) {
            LOG.error("Output repo cannot clean up data for execution ID '{}'", executionId, e);
        }
    }
}
