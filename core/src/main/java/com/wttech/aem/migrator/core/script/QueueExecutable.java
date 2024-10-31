package com.wttech.aem.migrator.core.script;

import com.wttech.aem.migrator.core.MigratorException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.sling.event.jobs.Job;

/** Represents an executable script (some version of it pushed onto the queue). */
public class QueueExecutable implements Executable {

    private final String id;

    private final String content;

    public QueueExecutable(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public static Map<String, Object> toJobProps(Executable executable) throws MigratorException {
        var props = new HashMap<String, Object>();
        props.put("id", executable.getId());
        props.put("content", executable.getContent());
        return props;
    }

    public static Executable fromJob(Job job) {
        return new QueueExecutable(job.getProperty("content", String.class), job.getProperty("id", String.class));
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String toString() {
        return new ToStringBuilder(this).append("id", id).toString();
    }
}
