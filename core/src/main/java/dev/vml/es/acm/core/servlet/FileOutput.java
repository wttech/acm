package dev.vml.es.acm.core.servlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class FileOutput implements Serializable {

    private List<File> files;

    public FileOutput(List<File> files) {
        this.files = files;
    }

    public List<File> getFiles() {
        return files;
    }
}
