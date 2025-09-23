package dev.vml.es.acm.core.servlet;

import java.io.Serializable;
import java.util.List;

public class FileOutput implements Serializable {

    private List<String> files;

    public FileOutput(List<String> files) {
        this.files = files;
    }

    public List<String> getFiles() {
        return files;
    }
}
