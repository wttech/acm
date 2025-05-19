package dev.vml.es.acm.core.code;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class OutputString implements Output {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    @Override
    public Optional<String> readString() {
        return Optional.of(new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
    }

    @Override
    public InputStream read() {
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Override
    public OutputStream write() {
        return outputStream;
    }

    @Override
    public void close() {
        try {
            outputStream.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
