package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.AcmException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

public abstract class Output implements Serializable {

    @JsonIgnore
    private final transient ByteArrayOutputStream stream = new ByteArrayOutputStream();

    private final String name;

    private String label;

    private String description;

    private String downloadName;

    private String mimeType = "application/octet-stream";

    public Output(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDownloadName() {
        return downloadName;
    }

    public void setDownloadName(String downloadName) {
        this.downloadName = downloadName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * TODO Use something more memory-efficient
     *
     * Something like: https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/output/UnsynchronizedByteArrayOutputStream.html
     * But available since commons-io 2.7 but AEM 6.5.0 uses only 2.6.
     */
    protected InputStream getInputStream() {
        return new ByteArrayInputStream(stream.toByteArray());
    }

    public OutputStream getStream() {
        return stream;
    }

    public void write(String text) {
        try {
            stream.write((text).getBytes());
        } catch (Exception ex) {
            throw new AcmException(String.format("Cannot write to output '%s'!", name), ex);
        }
    }

    public void writeln(String text) {
        write(text + "\n");
    }
}
