package dev.vml.es.acm.core.code;

import java.io.InputStream;
import java.io.OutputStream;

public interface CodeOutput {

    /**
     * Reads the current output as stream from the last flushed state.
     * This method is non-blocking and returns immediately.
     * For up-to-date content, call flush() before reading.
     */
    InputStream read();

    OutputStream write();

    /**
     * Ensures all pending writes are persisted/flushed.
     * This operation may block depending on the implementation.
     * Call this before reading if up-to-date content is required.
     */
    void flush();

    /**
     * Cleans up all used resources.
     */
    void close();
}
