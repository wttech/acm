package com.wttech.aem.contentor.core.utils;

import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OutputStreamWrapper extends OutputStream {

    private final OutputStream out;

    private final String appenderName;

    private final List<Logger> loggers;

    public OutputStreamWrapper(OutputStream out) {
        this.out = out;
        this.appenderName = UUID.randomUUID().toString();
        this.loggers = new ArrayList<>();
    }

    public String getAppenderName() {
        return appenderName;
    }

    public void registerLogger(Logger logger) {
        loggers.add(logger);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        loggers.forEach(logger -> logger.detachAppender(appenderName));
        out.close();
    }
}
