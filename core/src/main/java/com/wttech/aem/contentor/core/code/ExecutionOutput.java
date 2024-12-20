package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class ExecutionOutput {

  public static final String TMP_DIR = "contentor";

  public static Path path(String jobId) {
    File dir = FileUtils.getTempDirectory().toPath().resolve(TMP_DIR).toFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dir.toPath()
        .resolve(String.format("%s_output.txt", StringUtils.replace(jobId, "/", "-")));
  }

  public static Optional<String> readString(String jobId) throws ContentorException {
    Path path = path(jobId);
    if (!path.toFile().exists()) {
      return Optional.empty();
    }

    try (InputStream input = Files.newInputStream(path)) {
      return Optional.ofNullable(IOUtils.toString(input, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new ContentorException(
          String.format("Execution output file cannot be read as string for job '%s'", jobId), e);
    }
  }

  public static InputStream read(String jobId) {
    try {
      return Files.newInputStream(path(jobId));
    } catch (IOException e) {
      throw new ContentorException(
          String.format("Execution output file cannot be read for job '%s'", jobId), e);
    }
  }

  public static void delete(String jobId) throws ContentorException {
    try {
      Files.deleteIfExists(path(jobId));
    } catch (IOException e) {
      throw new ContentorException(
          String.format("Execution output file clean up failed for job '%s'", jobId), e);
    }
  }
}
