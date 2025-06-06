package dev.vml.es.acm.core.code;

import org.osgi.service.component.annotations.Component;

import java.io.File;
import java.io.InputStream;

@Component(immediate = true, service = FileManager.class)
public class FileManager {

    public File root() {
        return new File("/tmp/acm/file"); // TODO make it configurable
    }

    public File get(String path) {
        return new File(root(), path);
    }

    // write the file to the temporary directory to conventional path: /tmp/acm/file/yyyy/mm/dd/number/originalFileName.txt
    public File save(InputStream stream, String fileName) {
        return null;
    }
}
