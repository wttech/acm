package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.gui.SpaSettings;
import dev.vml.es.acm.core.osgi.OsgiContext;
import dev.vml.es.acm.core.repo.RepoChunks;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;

public class FileOutput extends Output implements Flushable, Closeable {

    @JsonIgnore
    private transient RepoChunks repoChunks;

    @JsonIgnore
    private transient PrintStream printStream;

    @JsonIgnore
    private transient ExecutionContext executionContext;

    private String downloadName;

    private String mimeType = "application/octet-stream";

    public FileOutput() {
        super(); // for deserialization
    }

    @Override
    public OutputType getType() {
        return OutputType.FILE;
    }

    public FileOutput(String name, ExecutionContext executionContext) {
        super(name);
        this.executionContext = executionContext;
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
            OsgiContext osgi = executionContext.getCodeContext().getOsgiContext();
            SpaSettings spaSettings = osgi.getService(SpaSettings.class);
            ResourceResolverFactory resolverFactory = osgi.getService(ResourceResolverFactory.class);
            String chunkFolderPath = String.format(
                    "%s/outputs/%s",
                    ExecutionContext.varPath(executionContext.getId()), StringUtils.replace(getName(), "/", "-"));
            repoChunks =
                    new RepoChunks(resolverFactory, chunkFolderPath, spaSettings.getExecutionFileOutputChunkSize());
        }
        return repoChunks;
    }

    @JsonIgnore
    public OutputStream getOutputStream() {
        return getRepoChunks().getOutputStream();
    }

    @JsonIgnore
    public InputStream getInputStream() {
        return getRepoChunks().getInputStream();
    }

    @JsonIgnore
    public PrintStream getOut() {
        if (printStream == null) {
            printStream = new PrintStream(getOutputStream());
        }
        return printStream;
    }

    @Override
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
