package dev.vml.es.acm.core.repo;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class RepoBuffer implements Closeable, Flushable {

    private static final String CHUNK_BASE_NAME = "chunk";

    private static final String CHUNK_MIME_TYPE = "application/octet-stream";

    private static final long FLUSH_ASYNC_TERMINATION_TIMEOUT_SECONDS = 5;

    private final RepoResource chunkFolder;

    private int chunkIndex = 1;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private ScheduledExecutorService flushAsyncScheduler;

    public RepoBuffer(RepoResource chunkFolder) {
        this.chunkFolder = chunkFolder;
    }

    public OutputStream getOutputStream() {
        return buffer;
    }

    public synchronized void flush() {
        if (buffer.size() == 0) {
            return;
        }
        chunkFolder.ensureRegularFolder();
        String chunkName = CHUNK_BASE_NAME + chunkIndex++;
        RepoResource chunk = chunkFolder.child(chunkName);
        chunk.saveFile(CHUNK_MIME_TYPE, new ByteArrayInputStream(buffer.toByteArray()));
        buffer.reset();
    }

    public void flushAsync(long intervalMillis) {
        if (flushAsyncScheduler != null) {
            throw new IllegalStateException(String.format("Repo buffer '%s' async flush already started!", chunkFolder.getPath()));
        } else {
            flushAsyncScheduler = Executors.newSingleThreadScheduledExecutor();
            flushAsyncScheduler.scheduleWithFixedDelay(this::flush, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void close() {
        if (flushAsyncScheduler != null) {
            flushAsyncScheduler.shutdown();
            try {
                flushAsyncScheduler.awaitTermination(FLUSH_ASYNC_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        chunkFolder.delete();
    }

    public InputStream getInputStream() {
        List<InputStream> streams = new ArrayList<>();
        chunkFolder.children()
            .filter(c -> c.getName().startsWith(CHUNK_BASE_NAME))
            .sorted(Comparator.comparing(c -> c.getName()))
            .forEachOrdered(c -> streams.add(c.readFileAsStream()));
        return streams.isEmpty()
            ? new ByteArrayInputStream(new byte[0])
            : new SequenceInputStream(Collections.enumeration(streams));
    }
}