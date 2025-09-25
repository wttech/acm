package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;

public class Output implements Serializable {

    @JsonIgnore
    private transient ByteArrayOutputStream dataStorage;

    @JsonIgnore
    private transient PrintStream printStream;

    private String name;

    private String label;

    private String description;

    private String downloadName;

    private String mimeType = "application/octet-stream";

    public Output() {
        // for deserialization
    }

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

    @JsonIgnore
    private ByteArrayOutputStream getDataStorage() {
        if (dataStorage == null) {
            dataStorage = new ByteArrayOutputStream();
        }
        return dataStorage;
    }

    /**
     * Get a raw output stream for binary data and formatters integration (e.g. JSON/YAML writers).
     */
    @JsonIgnore
    public OutputStream getOutputStream() {
        return getDataStorage();
    }

    /**
     * Get the input stream for reading the output data e.g. for saving in the execution history.
     */
    @JsonIgnore
    public InputStream getInputStream() {
        return new ByteArrayInputStream(getDataStorage().toByteArray());
    }

    /**
     * System.out-like print stream for text operations with auto-flush.
     * Use for println(), printf(), and formatted text output.
     */
    @JsonIgnore
    public PrintStream getOut() {
        if (printStream == null) {
            printStream = new PrintStream(getOutputStream(), true);
        }
        return printStream;
    }
}
