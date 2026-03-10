package dev.vml.es.acm.core.code.input;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.code.FileManager;
import dev.vml.es.acm.core.code.Input;
import dev.vml.es.acm.core.code.InputType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

abstract class AbstractFileInput<V> extends Input<V> {

    private List<String> mimeTypes;

    public AbstractFileInput(String name, InputType type, Class<?> valueType) {
        super(name, type, valueType);
    }

    protected void validatePath(String path) {
        if (StringUtils.isNotBlank(path) && !path.startsWith(FileManager.ROOT + "/")) {
            throw new AcmException(String.format(
                    "File input '%s' cannot have a value '%s' pointing outside ACM file storage '%s'. "
                            + "For selecting files from DAM or other repository locations, use path input instead.",
                    getName(), path, FileManager.ROOT));
        }
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeType(String mimeType) {
        this.mimeTypes = Collections.singletonList(mimeType);
    }

    public void setMimeTypes(List<String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public void images() {
        this.mimeTypes = Arrays.asList(
                "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp", "image/tiff", "image/svg+xml");
    }

    public void videos() {
        this.mimeTypes =
                Arrays.asList("video/mp4", "video/webm", "video/ogg", "video/avi", "video/mpeg", "video/quicktime");
    }

    public void audios() {
        this.mimeTypes = Arrays.asList("audio/mpeg", "audio/wav", "audio/ogg", "audio/aac", "audio/flac", "audio/mp3");
    }

    public void archives() {
        this.mimeTypes = Arrays.asList(
                "application/zip",
                "application/x-zip-compressed",
                "application/gzip",
                "application/x-gzip",
                "application/x-tar",
                "application/x-rar-compressed",
                "application/x-7z-compressed");
    }

    public void zips() {
        this.mimeTypes = Collections.singletonList("application/zip");
    }

    public void pdfs() {
        this.mimeTypes = Collections.singletonList("application/pdf");
    }
}
