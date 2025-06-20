package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class CodeOutputFile implements CodeOutput {

    public static final String TMP_DIR = "acm/output";

    private final String executionId;

    private final List<Closeable> closebles = new LinkedList<>();

    public CodeOutputFile(String executionId) {
        this.executionId = executionId;
    }

    public Path path() {
        File dir = FileUtils.getTempDirectory().toPath().resolve(TMP_DIR).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath().resolve(String.format("%s_output.txt", StringUtils.replace(executionId, "/", "-")));
    }

    @Override
    public Optional<String> readString() throws AcmException {
        Path path = path();
        if (!path.toFile().exists()) {
            return Optional.empty();
        }

        try (InputStream input = read()) {
            return Optional.ofNullable(IOUtils.toString(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Output file cannot be read as string for execution ID '%s'", executionId), e);
        }
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
