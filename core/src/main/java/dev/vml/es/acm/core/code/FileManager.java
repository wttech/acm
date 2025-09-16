package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = FileManager.class)
public class FileManager {

    public File root() {
        return FileUtils.getTempDirectory().toPath().resolve("acm/file").toFile();
    }

    public File get(String path) {
        try {
            File rootDir = root();
            File targetFile = new File(path);
            String canonicalRoot = rootDir.getCanonicalPath();
            String canonicalTarget = targetFile.getCanonicalPath();
            if (!canonicalTarget.startsWith(canonicalRoot + File.separator)) {
                throw new AcmException(
                        String.format("File path must be within the root directory '%s'!", canonicalRoot));
            }
            return targetFile;
        } catch (IOException e) {
            throw new AcmException("Error resolving file path: " + path, e);
        }
    }

    public File save(InputStream stream, String fileName) {
        try {
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String randomDir = System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
            File dir = new File(root(), datePath + "/" + randomDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new AcmException("File directory cannot be created: " + dir.getAbsolutePath());
            }
            File file = new File(dir, fileName);
            if (file.exists()) {
                throw new AcmException("File already exists: " + file.getAbsolutePath());
            }
            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                IOUtils.copy(stream, out);
            }
            return file;
        } catch (IOException e) {
            throw new AcmException("File cannot be saved: " + fileName, e);
        }
    }

    public File delete(String path) {
        File file = get(path);
        if (!file.exists()) {
            throw new AcmException(String.format("File to be deleted does not exist '%s'!", path));
        }
        if (!file.delete()) {
            throw new AcmException(String.format("File cannot be deleted '%s'!", path));
        }
        return file;
    }

    public List<File> deleteAll(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }
        return paths.stream().map(this::delete).collect(Collectors.toList());
    }
}
