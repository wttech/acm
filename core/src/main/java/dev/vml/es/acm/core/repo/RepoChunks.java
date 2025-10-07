package dev.vml.es.acm.core.repo;

import dev.vml.es.acm.core.util.ResolverUtils;
import java.io.*;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;

public class RepoChunks implements Closeable, Flushable {

    private static final String CHUNK_BASE_NAME = "chunk";

    private static final String CHUNK_MIME_TYPE = "application/octet-stream";

    private final ResourceResolverFactory resolverFactory;

    private final String chunkFolderPath;

    private final int chunkSize;

    private ChunkingOutputStream outputStream;

    private ChunkedInputStream inputStream;

    public RepoChunks(ResourceResolverFactory resolverFactory, String chunkFolderPath, int chunkSize) {
        this.resolverFactory = resolverFactory;
        this.chunkFolderPath = chunkFolderPath;
        this.chunkSize = chunkSize;
    }

    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new ChunkingOutputStream();
        }
        return outputStream;
    }

    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new ChunkedInputStream();
        }
        return inputStream;
    }

    @Override
    public void flush() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
                RepoResource chunkFolder = Repo.quiet(resolver).get(chunkFolderPath);
                if (chunkFolder.exists()) {
                    chunkFolder.delete();
                }
            });
        } catch (RuntimeException e) {
            throw new IOException(String.format("Repo chunks at path '%s' cannot be closed!", chunkFolderPath), e);
        }
    }

    private class ChunkingOutputStream extends OutputStream {

        private static final int LINE_LOOPBACK_CHARS_MAX = 1024;

        private final ByteArrayOutputStream buffer;

        private int chunkIndex = 1;

        ChunkingOutputStream() {
            this.buffer = new ByteArrayOutputStream(chunkSize);
        }

        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
            if (buffer.size() >= chunkSize) {
                createNewChunk();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (buffer.size() + len >= chunkSize) {
                int remainingSpace = chunkSize - buffer.size();
                if (remainingSpace > 0) {
                    buffer.write(b, off, remainingSpace);
                    createNewChunk();
                }

                int position = off + remainingSpace;
                while (position < off + len) {
                    int writeSize = Math.min(chunkSize, off + len - position);
                    buffer.write(b, position, writeSize);
                    if (buffer.size() >= chunkSize) {
                        createNewChunk();
                    }
                    position += writeSize;
                }
            } else {
                buffer.write(b, off, len);
            }
        }

        private void createNewChunk() throws IOException {
            byte[] data = buffer.toByteArray();
            final int pivot = findPivot(data);

            ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
                RepoResource chunkFolder = Repo.quiet(resolver).get(chunkFolderPath);
                chunkFolder.ensureRegularFolder();
                RepoResource chunk = chunkFolder.child(CHUNK_BASE_NAME + chunkIndex++);
                chunk.saveFile(CHUNK_MIME_TYPE, new ByteArrayInputStream(data, 0, pivot));
            });

            buffer.reset();
            if (pivot < data.length) {
                buffer.write(data, pivot, data.length - pivot);
            }
        }

        private int findPivot(byte[] data) {
            int start = Math.max(0, data.length - LINE_LOOPBACK_CHARS_MAX);
            for (int i = data.length - 1; i >= start; i--) {
                if (data[i] == '\n' || data[i] == '\r') return i + 1;
            }
            return data.length;
        }

        @Override
        public synchronized void flush() throws IOException {
            if (buffer.size() == 0) {
                return;
            }
            ResolverUtils.useContentResolver(resolverFactory, null, resolver -> {
                RepoResource chunkFolder = Repo.quiet(resolver).get(chunkFolderPath);
                chunkFolder.ensureRegularFolder();
                RepoResource chunk = chunkFolder.child(CHUNK_BASE_NAME + chunkIndex);
                chunk.saveFile(CHUNK_MIME_TYPE, new ByteArrayInputStream(buffer.toByteArray()));
            });
        }

        @Override
        public void close() throws IOException {
            flush();
            outputStream = null;
        }
    }

    private class ChunkedInputStream extends InputStream {

        private int currentChunkIndex = 1;

        private InputStream currentChunkStream;

        @Override
        public int read() throws IOException {
            if (currentChunkStream == null || currentChunkStream.available() == 0) {
                if (!moveToNextChunk()) {
                    return -1;
                }
            }
            return currentChunkStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (currentChunkStream == null || currentChunkStream.available() == 0) {
                if (!moveToNextChunk()) {
                    return -1;
                }
            }
            return currentChunkStream.read(b, off, len);
        }

        private boolean moveToNextChunk() {
            if (currentChunkStream != null) {
                try {
                    currentChunkStream.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            String.format(
                                    "Repo chunk '%d' at path '%s' cannot be closed!",
                                    currentChunkIndex, chunkFolderPath),
                            e);
                }
            }

            return ResolverUtils.queryContentResolver(resolverFactory, null, resolver -> {
                RepoResource chunkFolder = Repo.quiet(resolver).get(chunkFolderPath);
                RepoResource chunk = chunkFolder.child(CHUNK_BASE_NAME + currentChunkIndex++);
                if (!chunk.exists()) {
                    return false;
                }

                currentChunkStream = readToMemoryIfNeeded(chunk);

                return true;
            });
        }

        /**
         * If the chunk is not already in memory, read it completely while session is open.
         */
        private InputStream readToMemoryIfNeeded(RepoResource chunk) {
            InputStream sourceStream = chunk.readFileAsStream();
            if (!(sourceStream instanceof ByteArrayInputStream)) {
                try (InputStream stream = sourceStream) {
                    return new ByteArrayInputStream(IOUtils.toByteArray(stream));
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            String.format(
                                    "Repo chunk '%d' at path '%s' cannot be read into memory!",
                                    currentChunkIndex, chunkFolderPath),
                            e);
                }
            }
            return sourceStream;
        }

        @Override
        public void close() throws IOException {
            if (currentChunkStream != null) {
                currentChunkStream.close();
            }
            inputStream = null;
        }
    }
}
