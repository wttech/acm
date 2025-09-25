package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.HashMap;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.Job;

public class InputValues extends HashMap<String, Object> {

    public InputValues() {
        // for deserialization
    }

    public static InputValues fromJob(Job job) {
        try {
            return JsonUtils.readFromString(job.getProperty(ExecutionJob.INPUTS_PROP, String.class), InputValues.class);
        } catch (IOException e) {
            throw new AcmException("Cannot deserialize input values from JSON!", e);
        }
    }

    public InputValues(ValueMap values) {
        values.forEach((key, value) -> put(key, value));
    }
}
