package dev.vml.es.acm.core.code;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public interface CodeOutput {

    Optional<String> readString();

    InputStream read();

    OutputStream write();

    void close();
}
