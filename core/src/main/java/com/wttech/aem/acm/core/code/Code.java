package com.wttech.aem.acm.core.code;

import com.wttech.aem.acm.core.AcmException;
import com.wttech.aem.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

/**
 * Represents a code that can be executed.
 */
public class Code implements Executable {

    private String id;

    private String content;

    private ArgumentValues arguments;

    public Code() {
        // for deserialization
    }

    public Code(String id, String content, ArgumentValues arguments) {
        this.id = id;
        this.content = content;
        this.arguments = arguments;
    }

    public static Map<String, Object> toJobProps(Executable executable) throws AcmException {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("id", executable.getId());
            result.put("content", executable.getContent());
            result.put("arguments", JsonUtils.writeToString(executable.getArguments()));
            return result;
        } catch (IOException e) {
            throw new AcmException("Cannot serialize code to JSON!", e);
        }
    }

    public static Code fromJob(Job job) {
        try {
            String id = job.getProperty("id", String.class);
            String content = job.getProperty("content", String.class);
            ArgumentValues arguments =
                    JsonUtils.readFromString(job.getProperty("arguments", String.class), ArgumentValues.class);
            return new Code(id, content, arguments);
        } catch (IOException e) {
            throw new AcmException("Cannot deserialize code from JSON!", e);
        }
    }

    public static Code consoleMinimal() {
        Code result = new Code();
        result.id = "console";
        result.content = "boolean canRun() {\n" + "    return condition.always()\n"
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
    public ArgumentValues getArguments() {
        return arguments;
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .toString();
    }
}
