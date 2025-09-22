package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.RepoResource;
import dev.vml.es.acm.core.util.ResolverUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeOutputRepo implements CodeOutput {

    private static final Logger LOG = LoggerFactory.getLogger(CodeOutputRepo.class);

    private static final String MIME_TYPE = "text/plain";

    private static final String OUTPUT_DIR_NAME = "output";

    private static final String DATA_FILE_NAME = "data.txt";

    private static final int SCHEDULER_TERMINATION_TIMEOUT_SECONDS = 5;

    private static final int SCHEDULER_INITIAL_DELAY_SECONDS = 2;

    private static final int SCHEDULER_PERIOD_SECONDS = 2;

    private final ResourceResolverFactory resolverFactory;

    private final String executionId;

    private final ByteArrayOutputStream buffer;

    private ScheduledExecutorService scheduler;

    public CodeOutputRepo(ResourceResolverFactory resolverFactory, String executionId) {
        this.resolverFactory = resolverFactory;
        this.executionId = executionId;
        this.buffer = new ByteArrayOutputStream();
    }

    private void startAsyncSave() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                    this::saveToRepo, SCHEDULER_INITIAL_DELAY_SECONDS, SCHEDULER_PERIOD_SECONDS, TimeUnit.SECONDS);
        }
    }

    private RepoResource getDataResource(ResourceResolver resolver) {
        String path = String.format("%s/%s/%s/%s", AcmConstants.VAR_ROOT, OUTPUT_DIR_NAME, executionId, DATA_FILE_NAME);
        return RepoResource.of(resolver, path);
    }

    private void saveToRepo() {
        byte[] data = buffer.toByteArray();
        if (data.length == 0) {
            return;
        }

        try {
            ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
                RepoResource dataResource = getDataResource(resolver);
                dataResource.parent().ensureRegularFolder();
                dataResource.saveFile(MIME_TYPE, new ByteArrayInputStream(data));
            });
        } catch (Exception e) {
            LOG.error("Output repo cannot save data for execution ID '{}'", executionId, e);
        }
    }

    @Override
    public Optional<String> readString() throws AcmException {
        try {
            return ResolverUtils.queryContentResolver(resolverFactory, null, resolver -> {
                RepoResource resource = getDataResource(resolver);
                if (!resource.exists()) {
                    return Optional.empty();
                }
                try (InputStream stream = resource.readFileAsStream()) {
                    return Optional.of(IOUtils.toString(stream, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new AcmException(
                            String.format("Output repo cannot read as string for execution ID '%s'", executionId), e);
                }
            });
        } catch (Exception e) {
            throw new AcmException(
                    String.format("Output repo cannot read as string for execution ID '%s'", executionId), e);
        }
    }

    @Override
    public InputStream read() {
        try {
            return ResolverUtils.queryContentResolver(resolverFactory, null, resolver -> {
                RepoResource resource = getDataResource(resolver);
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
    public void close() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(SCHEDULER_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            saveToRepo();
        }
    }
}
