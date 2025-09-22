package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class CodeOutputFile implements CodeOutput {

    public static final String OUTPUT_DIRNAME = "output";

    private final File dir;

    private final String executionId;

    private final List<Closeable> closebles = new LinkedList<>();

    public CodeOutputFile(FileManager fileManager, String executionId) {
        this.dir = fileManager.tempDir().toPath().resolve(OUTPUT_DIRNAME).toFile();
        this.executionId = executionId;
    }

    public Path path() {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath().resolve(String.format("%s_output.txt", StringUtils.replace(executionId, "/", "-")));
    }

    @Override
    public InputStream read() {
        try {
            InputStream result = Files.newInputStream(path());
            closebles.add(result);
            return result;
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Output file cannot open for reading for execution ID '%s'", executionId), e);
        }
    }

    @Override
    public OutputStream write() {
        try {
            OutputStream result = Files.newOutputStream(path());
            closebles.add(result);
            return result;
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Output file cannot be open for writing for execution ID '%s'", executionId), e);
        }
    }

    @Override
    public void flush() {
        for (Closeable closeable : closebles) {
            if (closeable instanceof OutputStream) {
                try {
                    ((OutputStream) closeable).flush();
                } catch (IOException e) {
                    // ignore flush errors
                }
            }
        }
    }

    public void delete() throws AcmException {
        try {
            Files.deleteIfExists(path());
        } catch (IOException e) {
            throw new AcmException(String.format("Output file clean up failed for execution ID '%s'", executionId), e);
        }
    }

    @Override
    public void close() {
        for (Closeable closeable : closebles) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
        closebles.clear();
        delete();
    }
}
