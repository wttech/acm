package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Closeable;
import java.io.Flushable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;

public abstract class Output implements Serializable, Flushable, Closeable {

    private String name;

    private String label;

    private String description;

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

    /**
     * Get a raw output stream for binary data and formatters integration (e.g. JSON/YAML writers).
     */
    @JsonIgnore
    public abstract OutputStream getOutputStream();

    /**
     * Get the input stream for reading the output data e.g. for saving in the execution history.
     */
    @JsonIgnore
    public abstract InputStream getInputStream();

    /**
     * System.out-like print stream for text operations.
     * Use for println(), printf(), and formatted text output.
     */
    @JsonIgnore
    public abstract PrintStream getOut();
}
