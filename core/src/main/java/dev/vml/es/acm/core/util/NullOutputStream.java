package dev.vml.es.acm.core.util;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream {

    @Override
    public void write(final byte[] b, final int off, final int len) {
        // do nothing
    }

    @Override
    public void write(final int b) {
        // do nothing
    }

    @Override
    public void write(final byte[] b) throws IOException {
        // do nothing
    }
}
