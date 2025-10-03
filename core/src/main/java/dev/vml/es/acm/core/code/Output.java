package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.repo.RepoChunks;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;

public class Output implements Serializable, Flushable, AutoCloseable {

    @JsonIgnore
    private transient RepoChunks repoChunks;

    @JsonIgnore
    private transient PrintStream printStream;

    @JsonIgnore
    private transient ExecutionContext executionContext;

    private String name;

    private String label;

    private String description;

    private String downloadName;

    private String mimeType = "application/octet-stream";

    public Output() {
        // for deserialization
    }

    public Output(String name, ExecutionContext executionContext) {
        this.name = name;
        this.executionContext = executionContext;
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
    private RepoChunks getRepoChunks() {
        if (repoChunks == null) {
            ResourceResolverFactory resolverFactory =
                    executionContext.getCodeContext().getOsgiContext().getService(ResourceResolverFactory.class);
            String chunkFolderPath = String.format(
                    "%s/output/%s/%s",
                    AcmConstants.VAR_ROOT,
                    StringUtils.replace(executionContext.getId(), "/", "-"),
                    StringUtils.replace(name, "/", "-"));
            repoChunks = new RepoChunks(resolverFactory, chunkFolderPath);
        }
        return repoChunks;
    }

    /**
     * Get a raw output stream for binary data and formatters integration (e.g. JSON/YAML writers).
     */
    @JsonIgnore
    public OutputStream getOutputStream() {
        return getRepoChunks().getOutputStream();
    }

    /**
     * Get the input stream for reading the output data e.g. for saving in the execution history.
     */
    @JsonIgnore
    public InputStream getInputStream() {
        return getRepoChunks().getInputStream();
    }

    /**
     * System.out-like print stream for text operations with auto-flush.
     * Use for println(), printf(), and formatted text output.
     */
    @JsonIgnore
    public PrintStream getOut() {
        if (printStream == null) {
            printStream = new PrintStream(getOutputStream());
        }
        return printStream;
    }

    /**
     * Free the memory, write data to the repository.
     */
    public void flush() throws IOException {
        if (printStream != null) {
            printStream.flush();
        }
        getRepoChunks().flush();
    }

    @Override
    public void close() throws IOException {
        getRepoChunks().close();
    }
}
