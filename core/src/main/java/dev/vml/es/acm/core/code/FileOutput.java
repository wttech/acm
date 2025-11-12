package dev.vml.es.acm.core.code;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vml.es.acm.core.AcmException;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class FileOutput extends Output implements Flushable, Closeable {

    private static final String TEMP_DIR = "acm/execution/file";

    @JsonIgnore
    private transient File tempFile;

    @JsonIgnore
    private transient FileOutputStream fileOutputStream;

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
    private File getTempFile() {
        if (tempFile == null) {
            File tmpDir = FileUtils.getTempDirectory();
            String contextPrefix = StringUtils.replace(executionContext.getId(), "/", "-");
            String sanitizedName = StringUtils.replace(getName(), "/", "-");
            String fileName = String.format("%s_%s.out", contextPrefix, sanitizedName);
            File tempFile = new File(new File(tmpDir, TEMP_DIR), fileName);
            File parentDir = tempFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                try {
                    FileUtils.forceMkdir(parentDir);
                } catch (IOException e) {
                    throw new AcmException(String.format("Cannot create temp directory for output '%s'", getName()), e);
                }
            }
            this.tempFile = tempFile;
        }
        return tempFile;
    }

    @JsonIgnore
    public OutputStream getOutputStream() {
        if (fileOutputStream == null) {
            try {
                fileOutputStream = new FileOutputStream(getTempFile());
            } catch (IOException e) {
                throw new AcmException(String.format("Cannot create output stream for file output '%s'", getName()), e);
            }
        }
        return fileOutputStream;
    }

    @JsonIgnore
    public InputStream getInputStream() {
        try {
            return new FileInputStream(getTempFile());
        } catch (IOException e) {
            throw new AcmException(String.format("Cannot read file output '%s'", getName()), e);
        }
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
        if (fileOutputStream != null) {
            fileOutputStream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (printStream != null) {
            printStream.close();
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        if (tempFile != null && tempFile.exists()) {
            FileUtils.forceDelete(tempFile);
        }
    }
}
