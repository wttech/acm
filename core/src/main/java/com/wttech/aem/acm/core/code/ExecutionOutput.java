package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
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

public class ExecutionOutput {

    public static final String TMP_DIR = "acm";

    private final String jobId;

    private final List<Closeable> closebles = new LinkedList<>();

    public ExecutionOutput(String jobId) {
        this.jobId = jobId;
    }

    public Path path() {
        File dir = FileUtils.getTempDirectory().toPath().resolve(TMP_DIR).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.toPath().resolve(String.format("%s_output.txt", StringUtils.replace(jobId, "/", "-")));
    }

    public Optional<String> readString() throws AcmException {
        Path path = path();
        if (!path.toFile().exists()) {
            return Optional.empty();
        }

        try (InputStream input = read()) {
            return Optional.ofNullable(IOUtils.toString(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Execution output file cannot be read as string for job '%s'", jobId), e);
        }
    }

    public InputStream read() {
        try {
            InputStream result = Files.newInputStream(path());
            closebles.add(result);
            return result;
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Execution output file cannot open for reading for job '%s'", jobId), e);
        }
    }

    public OutputStream write() {
        try {
            OutputStream result = Files.newOutputStream(path());
            closebles.add(result);
            return result;
        } catch (IOException e) {
            throw new AcmException(
                    String.format("Execution output file cannot be open for writing for job '%s'", jobId), e);
        }
    }

    public void delete() throws AcmException {
        try {
            Files.deleteIfExists(path());
        } catch (IOException e) {
            throw new AcmException(String.format("Execution output file clean up failed for job '%s'", jobId), e);
        }
    }

    public void close() {
        for (Closeable closeable : closebles) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
        closebles.clear();
    }
}
