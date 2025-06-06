package dev.vml.es.acm.core.servlet;

import java.io.File;
import java.io.Serializable;

public class FileOutput implements Serializable {

    private File file;

    public FileOutput(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }
}
