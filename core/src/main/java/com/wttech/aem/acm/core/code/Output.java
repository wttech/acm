package com.wttech.aem.acm.core.code;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public interface Output {

    Optional<String> readString();

    InputStream read();

    OutputStream write();

    void close();
}
