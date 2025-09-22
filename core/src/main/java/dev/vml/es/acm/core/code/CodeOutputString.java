package dev.vml.es.acm.core.code;

import java.io.*;

public class CodeOutputString implements CodeOutput {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

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

    @Override
    public void flush() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            // ignore
        }
    }
}
