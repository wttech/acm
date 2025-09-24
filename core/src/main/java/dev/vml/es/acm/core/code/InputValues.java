package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.util.JsonUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.Job;

public class InputValues extends HashMap<String, Object> {

    public InputValues(ValueMap values) {
        values.forEach((key, value) -> put(key, value));
    }

    public static Map<String, Object> toJobProps(InputValues values) throws AcmException {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put(ExecutionJob.EXECUTABLE_INPUTS_PROP, JsonUtils.writeToString(values));
            return result;
        } catch (IOException e) {
            throw new AcmException("Cannot serialize input values to JSON!", e);
        }
    }

    public static InputValues fromJob(Job job) {
        try {
            return JsonUtils.readFromString(
                    job.getProperty(ExecutionJob.EXECUTABLE_INPUTS_PROP, String.class), InputValues.class);
        } catch (IOException e) {
            throw new AcmException("Cannot deserialize input values from JSON!", e);
        }
    }
}
