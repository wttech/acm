package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

/**
 * Represents any code (e.g text from interactive console) that can be executed.
 */
public class Code implements Executable {

    private String id;

    private String content;

    public Code() {
        // for deserialization
    }

    public Code(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public static Map<String, Object> toJobProps(Executable executable) throws AcmException {
        Map<String, Object> result = new HashMap<>();
        result.put(ExecutionJob.EXECUTABLE_ID_PROP, executable.getId());
        result.put(ExecutionJob.EXECUTABLE_CONTENT_PROP, executable.getContent());
        return result;
    }

    public static Code fromJob(Job job) {

        String id = job.getProperty(ExecutionJob.EXECUTABLE_ID_PROP, String.class);
        String content = job.getProperty(ExecutionJob.EXECUTABLE_CONTENT_PROP, String.class);
        return new Code(id, content);
    }

    public static Code consoleMinimal() {
        Code result = new Code();
        result.id = "console";
        result.content = "boolean canRun() {\n" + "    return conditions.always()\n"
                + "  }\n"
                + "    \n"
                + "  void doRun() {\n"
                + "    println \"Hello World!\"\n"
                + "  }";
        return result;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public ExecutableMetadata getMetadata() {
        return ExecutableMetadata.of(this);
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .toString();
    }
}
