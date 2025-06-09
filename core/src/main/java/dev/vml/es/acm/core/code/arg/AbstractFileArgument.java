package dev.vml.es.acm.core.code.arg;

import dev.vml.es.acm.core.code.Argument;
import dev.vml.es.acm.core.code.ArgumentType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract class AbstractFileArgument<V> extends Argument<V> {

    private List<String> mimeTypes;

    public AbstractFileArgument(String name, ArgumentType type, Class<?> valueType) {
        super(name, type, valueType);
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
                "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp", "image/tiff", "image/svg+xml"
        );
    }

    public void videos() {
        this.mimeTypes = Arrays.asList(
                "video/mp4", "video/webm", "video/ogg", "video/avi", "video/mpeg", "video/quicktime"
        );
    }

    public void audios() {
        this.mimeTypes = Arrays.asList("audio/mpeg", "audio/wav", "audio/ogg", "audio/aac", "audio/flac", "audio/mp3");
    }

    public void archives() {
        this.mimeTypes = Arrays.asList(
                "application/zip", "application/x-zip-compressed", "application/gzip", "application/x-gzip",
                "application/x-tar", "application/x-rar-compressed", "application/x-7z-compressed"
        );
    }

    public void zips() {
        this.mimeTypes = Collections.singletonList("application/zip");
    }

    public void pdfs() {
        this.mimeTypes = Collections.singletonList("application/pdf");
    }
}
