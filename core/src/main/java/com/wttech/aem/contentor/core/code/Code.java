package com.wttech.aem.contentor.core.code;

import com.wttech.aem.contentor.core.ContentorException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.sling.event.jobs.Job;

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

  public static Map<String, Object> toJobProps(Executable executable) throws ContentorException {
    Map<String, Object> props = new HashMap<>();
    props.put("id", executable.getId());
    props.put("content", executable.getContent());
    return props;
  }

  public static Code fromJob(Job job) {
    return new Code(job.getProperty("id", String.class), job.getProperty("content", String.class));
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getContent() {
    return content;
  }

  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("id", id).toString();
  }
}
